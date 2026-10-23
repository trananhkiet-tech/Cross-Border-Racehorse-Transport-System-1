package com.example.racetransport.common.exception;

import org.springframework.http.HttpStatus;

/** Hành động không hợp lệ với trạng thái hiện tại (409). */
public class InvalidStateException extends ApiException {

    public InvalidStateException(String message) {
        super(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }
}
