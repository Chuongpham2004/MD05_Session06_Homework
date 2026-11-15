package org.example.transactionservice.client.dto;

import java.math.BigDecimal;

public record TransferBalanceRequest(
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount
) {
}
