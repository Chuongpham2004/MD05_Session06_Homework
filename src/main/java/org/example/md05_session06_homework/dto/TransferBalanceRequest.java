package org.example.md05_session06_homework.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Request nội bộ từ transaction-service: trừ tiền tài khoản nguồn, cộng tiền tài khoản đích. */
public record TransferBalanceRequest(
        @NotBlank(message = "Số tài khoản nguồn không được để trống")
        String sourceAccountNumber,

        @NotBlank(message = "Số tài khoản đích không được để trống")
        String destinationAccountNumber,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
        BigDecimal amount
) {
}
