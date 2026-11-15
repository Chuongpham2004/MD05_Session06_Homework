package org.example.transactionservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ khi chuyển tiền, mang theo HTTP status và id giao dịch FAILED đã ghi lại. */
@Getter
public class TransferException extends RuntimeException {

    private final HttpStatus status;
    private final Long transactionId;

    public TransferException(HttpStatus status, String message, Long transactionId) {
        super(message);
        this.status = status;
        this.transactionId = transactionId;
    }
}
