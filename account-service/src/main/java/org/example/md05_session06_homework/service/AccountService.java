package org.example.md05_session06_homework.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.md05_session06_homework.dto.AccountResponse;
import org.example.md05_session06_homework.dto.CreateAccountRequest;
import org.example.md05_session06_homework.dto.TransferBalanceRequest;
import org.example.md05_session06_homework.entity.Account;
import org.example.md05_session06_homework.entity.AccountStatus;
import org.example.md05_session06_homework.exception.AccountNotFoundException;
import org.example.md05_session06_homework.exception.BusinessRuleException;
import org.example.md05_session06_homework.exception.DuplicateResourceException;
import org.example.md05_session06_homework.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String email = request.email().trim().toLowerCase();
        if (accountRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email " + email + " đã được sử dụng");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .ownerName(request.ownerName().trim())
                .email(email)
                .balance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException("Không tìm thấy tài khoản với id " + id));
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(AccountResponse::from)
                .orElseThrow(() -> new AccountNotFoundException("Không tìm thấy tài khoản số " + accountNumber));
    }

    /**
     * Trừ tiền tài khoản nguồn và cộng tiền tài khoản đích trong CÙNG một transaction DB,
     * nên không bao giờ xảy ra trường hợp trừ rồi mà không cộng.
     */
    @Transactional
    public void transferBalance(TransferBalanceRequest request) {
        String sourceNo = request.sourceAccountNumber();
        String destNo = request.destinationAccountNumber();
        if (sourceNo.equals(destNo)) {
            throw new BusinessRuleException("Tài khoản nguồn và tài khoản đích không được trùng nhau");
        }

        // Khóa theo thứ tự số tài khoản để tránh deadlock khi 2 giao dịch ngược chiều chạy cùng lúc
        String first = sourceNo.compareTo(destNo) < 0 ? sourceNo : destNo;
        String second = first.equals(sourceNo) ? destNo : sourceNo;
        Account a1 = lockAccount(first);
        Account a2 = lockAccount(second);
        Account source = a1.getAccountNumber().equals(sourceNo) ? a1 : a2;
        Account destination = source == a1 ? a2 : a1;

        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Tài khoản nguồn " + sourceNo + " không ở trạng thái ACTIVE");
        }
        if (destination.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Tài khoản đích " + destNo + " không ở trạng thái ACTIVE");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessRuleException("Số dư tài khoản " + sourceNo + " không đủ để chuyển " + request.amount());
        }

        source.setBalance(source.getBalance().subtract(request.amount()));
        destination.setBalance(destination.getBalance().add(request.amount()));
        log.info("Đã chuyển {} từ {} (còn {}) sang {} (còn {})", request.amount(),
                sourceNo, source.getBalance(), destNo, destination.getBalance());
    }

    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Không tìm thấy tài khoản số " + accountNumber));
    }

    /** Sinh số tài khoản 12 chữ số, bắt đầu bằng "1000", đảm bảo không trùng. */
    private String generateAccountNumber() {
        String number;
        do {
            number = "1000" + String.format("%08d", RANDOM.nextInt(100_000_000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
