package com.example.racetransport.common.exception;

import org.springframework.http.HttpStatus;

/** Xung đột dữ liệu, ví dụ trùng mã duy nhất (409). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
