package org.example.md05_session06_homework.dto;

import org.example.md05_session06_homework.entity.Account;
import org.example.md05_session06_homework.entity.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        String email,
        BigDecimal balance,
        AccountStatus status,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getOwnerName(),
                a.getEmail(), a.getBalance(), a.getStatus(), a.getCreatedAt());
    }
}
