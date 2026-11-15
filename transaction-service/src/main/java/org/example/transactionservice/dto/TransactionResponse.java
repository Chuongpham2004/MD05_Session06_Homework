package org.example.transactionservice.dto;

import org.example.transactionservice.entity.Transaction;
import org.example.transactionservice.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String requestId,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String description,
        TransactionStatus status,
        String failureReason,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getRequestId(), t.getSourceAccountNumber(),
                t.getDestinationAccountNumber(), t.getAmount(), t.getDescription(),
                t.getStatus(), t.getFailureReason(), t.getCreatedAt());
    }
}
