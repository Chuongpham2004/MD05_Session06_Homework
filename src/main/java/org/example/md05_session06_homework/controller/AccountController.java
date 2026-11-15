package org.example.md05_session06_homework.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.md05_session06_homework.dto.AccountResponse;
import org.example.md05_session06_homework.dto.CreateAccountRequest;
import org.example.md05_session06_homework.dto.TransferBalanceRequest;
import org.example.md05_session06_homework.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.createAccount(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/number/{accountNumber}")
    public AccountResponse getAccountByNumber(@PathVariable String accountNumber) {
        log.info("[account-service] Nhận yêu cầu kiểm tra tài khoản số {}", accountNumber);
        AccountResponse account = accountService.getAccountByNumber(accountNumber);
        log.info("[account-service] Tài khoản {} tồn tại, chủ TK: {}, trạng thái: {}",
                accountNumber, account.ownerName(), account.status());
        return account;
    }

    /** API nội bộ cho transaction-service: trừ/cộng số dư 2 tài khoản trong 1 transaction. */
    @PostMapping("/transfer")
    public ResponseEntity<Void> transferBalance(@Valid @RequestBody TransferBalanceRequest request) {
        log.info("[account-service] Nhận yêu cầu cập nhật số dư: {} -> {}, số tiền {}",
                request.sourceAccountNumber(), request.destinationAccountNumber(), request.amount());
        accountService.transferBalance(request);
        return ResponseEntity.noContent().build();
    }
}
