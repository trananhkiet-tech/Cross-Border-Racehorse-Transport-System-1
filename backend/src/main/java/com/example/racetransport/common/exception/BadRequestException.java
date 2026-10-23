package com.example.racetransport.common.exception;

import org.springframework.http.HttpStatus;

/** Dữ liệu không hợp lệ (400). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}
