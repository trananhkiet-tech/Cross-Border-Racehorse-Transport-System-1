package com.example.racetransport.common.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Định dạng lỗi thống nhất cho mọi API.
 * Không chứa stack trace hay thông tin nhạy cảm.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public record FieldViolation(String field, String message) { }
}
