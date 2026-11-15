package org.example.transactionservice.client.dto;

import java.math.BigDecimal;

/** Dữ liệu tài khoản trả về từ account-service (chỉ lấy các trường cần dùng). */
public record AccountDto(
        Long id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        String status
) {
}
