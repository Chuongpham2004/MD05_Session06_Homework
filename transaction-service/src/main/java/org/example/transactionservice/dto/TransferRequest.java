package org.example.transactionservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message = "Số tài khoản nguồn không được để trống")
        String sourceAccountNumber,

        @NotBlank(message = "Số tài khoản đích không được để trống")
        String destinationAccountNumber,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 17, fraction = 2, message = "Số tiền tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
        BigDecimal amount,

        @Size(max = 255, message = "Nội dung tối đa 255 ký tự")
        String description
) {
}
