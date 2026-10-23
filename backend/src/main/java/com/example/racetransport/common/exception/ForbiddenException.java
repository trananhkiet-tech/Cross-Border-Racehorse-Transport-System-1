package com.example.racetransport.common.exception;

import org.springframework.http.HttpStatus;

/** Không có quyền truy cập dữ liệu này (403). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
