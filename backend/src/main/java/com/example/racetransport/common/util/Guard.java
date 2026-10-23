package com.example.racetransport.common.util;

import com.example.racetransport.common.exception.BadRequestException;
import com.example.racetransport.common.exception.InvalidStateException;

/** Kiểm tra điều kiện nghiệp vụ ngắn gọn trong service. */
public final class Guard {

    private Guard() { }

    /** Ném 409 INVALID_STATE nếu điều kiện trạng thái không thỏa. */
    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new InvalidStateException(message);
        }
    }

    /** Ném 400 BAD_REQUEST nếu dữ liệu đầu vào không thỏa quy tắc nghiệp vụ. */
    public static void input(boolean condition, String message) {
        if (!condition) {
            throw new BadRequestException(message);
        }
    }
}
