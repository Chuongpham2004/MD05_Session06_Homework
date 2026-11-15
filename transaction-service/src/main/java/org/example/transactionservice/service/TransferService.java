package org.example.transactionservice.service;

import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.client.AccountClient;
import org.example.transactionservice.client.dto.AccountDto;
import org.example.transactionservice.client.dto.TransferBalanceRequest;
import org.example.transactionservice.config.RequestIdFilter;
import org.example.transactionservice.dto.TransactionResponse;
import org.example.transactionservice.dto.TransferRequest;
import org.example.transactionservice.entity.Transaction;
import org.example.transactionservice.entity.TransactionStatus;
import org.example.transactionservice.exception.TransactionNotFoundException;
import org.example.transactionservice.exception.TransferException;
import org.example.transactionservice.repository.TransactionRepository;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Pattern MESSAGE_FIELD = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final AccountClient accountClient;
    private final TransactionRepository transactionRepository;

    public TransactionResponse transfer(TransferRequest request) {
        String sourceNo = request.sourceAccountNumber().trim();
        String destNo = request.destinationAccountNumber().trim();
        log.info("[transaction-service] Nhận yêu cầu chuyển {} từ {} sang {}", request.amount(), sourceNo, destNo);

        if (sourceNo.equals(destNo)) {
            throw fail(request, HttpStatus.BAD_REQUEST, "Tài khoản nguồn và tài khoản đích không được trùng nhau");
        }

        // Bước 1: gọi account-service kiểm tra TÀI KHOẢN NGUỒN có tồn tại không (bắt buộc trước khi xử lý)
        log.info("[transaction-service] Gọi account-service kiểm tra tài khoản nguồn {}", sourceNo);
        AccountDto source = fetchAccount(request, sourceNo, "nguồn");
        log.info("[transaction-service] Tài khoản nguồn hợp lệ: {} - {}, số dư {}",
                source.accountNumber(), source.ownerName(), source.balance());

        // Bước 2: kiểm tra tài khoản đích
        log.info("[transaction-service] Gọi account-service kiểm tra tài khoản đích {}", destNo);
        AccountDto destination = fetchAccount(request, destNo, "đích");
        log.info("[transaction-service] Tài khoản đích hợp lệ: {} - {}", destination.accountNumber(), destination.ownerName());

        // Bước 3: kiểm tra nhanh trạng thái và số dư (account-service vẫn kiểm tra lại khi cập nhật, có khóa dòng)
        if (!"ACTIVE".equals(source.status())) {
            throw fail(request, HttpStatus.UNPROCESSABLE_CONTENT, "Tài khoản nguồn " + sourceNo + " không ở trạng thái ACTIVE");
        }
        if (source.balance().compareTo(request.amount()) < 0) {
            throw fail(request, HttpStatus.UNPROCESSABLE_CONTENT,
                    "Số dư tài khoản " + sourceNo + " không đủ (hiện có " + source.balance() + ")");
        }

        // Bước 4: yêu cầu account-service trừ/cộng số dư trong 1 transaction
        log.info("[transaction-service] Gọi account-service cập nhật số dư");
        try {
            accountClient.transferBalance(new TransferBalanceRequest(sourceNo, destNo, request.amount()));
        } catch (RetryableException e) {
            throw fail(request, HttpStatus.SERVICE_UNAVAILABLE,
                    "account-service không phản hồi khi cập nhật số dư, cần đối soát lại giao dịch");
        } catch (FeignException e) {
            throw fail(request, mapStatus(e), extractMessage(e));
        }

        Transaction saved = transactionRepository.save(newTransaction(request, TransactionStatus.SUCCESS, null));
        log.info("[transaction-service] Chuyển tiền THÀNH CÔNG, mã giao dịch #{}", saved.getId());
        return TransactionResponse.from(saved);
    }

    public List<TransactionResponse> getAll() {
        return transactionRepository.findAllByOrderByIdDesc().stream().map(TransactionResponse::from).toList();
    }

    public TransactionResponse getById(Long id) {
        return transactionRepository.findById(id).map(TransactionResponse::from)
                .orElseThrow(() -> new TransactionNotFoundException("Không tìm thấy giao dịch #" + id));
    }

    private AccountDto fetchAccount(TransferRequest request, String accountNumber, String role) {
        try {
            return accountClient.getAccountByNumber(accountNumber);
        } catch (FeignException.NotFound e) {
            throw fail(request, HttpStatus.NOT_FOUND, "Tài khoản " + role + " " + accountNumber + " không tồn tại");
        } catch (RetryableException e) {
            throw fail(request, HttpStatus.SERVICE_UNAVAILABLE, "Không kết nối được account-service: " + e.getMessage());
        } catch (FeignException e) {
            throw fail(request, HttpStatus.BAD_GATEWAY, "account-service trả lỗi: " + extractMessage(e));
        }
    }

    /** Ghi lại giao dịch FAILED để đối soát rồi trả về exception cho controller. */
    private TransferException fail(TransferRequest request, HttpStatus status, String reason) {
        Transaction failed = transactionRepository.save(newTransaction(request, TransactionStatus.FAILED, reason));
        log.warn("[transaction-service] Chuyển tiền THẤT BẠI (#{}): {}", failed.getId(), reason);
        return new TransferException(status, reason, failed.getId());
    }

    private Transaction newTransaction(TransferRequest request, TransactionStatus status, String reason) {
        return Transaction.builder()
                .requestId(MDC.get(RequestIdFilter.MDC_KEY))
                .sourceAccountNumber(request.sourceAccountNumber().trim())
                .destinationAccountNumber(request.destinationAccountNumber().trim())
                .amount(request.amount())
                .description(request.description())
                .status(status)
                .failureReason(reason)
                .build();
    }

    private HttpStatus mapStatus(FeignException e) {
        HttpStatus status = HttpStatus.resolve(e.status());
        return status != null && status.is4xxClientError() ? status : HttpStatus.BAD_GATEWAY;
    }

    /** Lấy trường "message" trong body lỗi JSON của account-service. */
    private String extractMessage(FeignException e) {
        Matcher m = MESSAGE_FIELD.matcher(e.contentUTF8());
        return m.find() ? m.group(1) : "account-service trả về HTTP " + e.status();
    }
}
