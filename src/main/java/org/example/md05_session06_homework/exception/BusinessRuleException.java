package org.example.md05_session06_homework.exception;

/** Vi phạm quy tắc nghiệp vụ (không đủ số dư, tài khoản bị khóa, ...) -> 422. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
