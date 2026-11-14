package org.example.md05_session06_homework.service;

import lombok.RequiredArgsConstructor;
import org.example.md05_session06_homework.dto.AccountResponse;
import org.example.md05_session06_homework.dto.CreateAccountRequest;
import org.example.md05_session06_homework.entity.Account;
import org.example.md05_session06_homework.entity.AccountStatus;
import org.example.md05_session06_homework.exception.AccountNotFoundException;
import org.example.md05_session06_homework.exception.DuplicateResourceException;
import org.example.md05_session06_homework.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

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

    /** Sinh số tài khoản 12 chữ số, bắt đầu bằng "1000", đảm bảo không trùng. */
    private String generateAccountNumber() {
        String number;
        do {
            number = "1000" + String.format("%08d", RANDOM.nextInt(100_000_000));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
