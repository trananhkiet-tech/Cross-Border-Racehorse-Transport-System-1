package com.example.racetransport.common.exception;

import org.springframework.http.HttpStatus;

/** Không tìm thấy dữ liệu (404). */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
