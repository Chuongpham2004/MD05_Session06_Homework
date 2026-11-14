package org.example.md05_session06_homework.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "Tên chủ tài khoản không được để trống")
        @Size(max = 100, message = "Tên chủ tài khoản tối đa 100 ký tự")
        String ownerName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @DecimalMin(value = "0.0", message = "Số dư ban đầu không được âm")
        @Digits(integer = 17, fraction = 2, message = "Số dư tối đa 17 chữ số phần nguyên và 2 chữ số thập phân")
        BigDecimal initialBalance
) {
}
