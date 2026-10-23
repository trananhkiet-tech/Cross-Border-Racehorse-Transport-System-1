package com.example.racetransport.common.exception;

import org.springframework.http.HttpStatus;

/** Ngoại lệ nghiệp vụ có HTTP status và mã lỗi ổn định cho frontend. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }

    public String getCode() { return code; }
}
