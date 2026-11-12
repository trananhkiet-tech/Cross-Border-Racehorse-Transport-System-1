package com.example.racetransport.horse.entity;

/**
 * HorseStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum HorseStatus {
    /** Đang hoạt động */
    ACTIVE("Đang hoạt động"),
    /** Đang vận chuyển */
    IN_TRANSIT("Đang vận chuyển"),
    /** Đã lưu trữ */
    ARCHIVED("Đã lưu trữ");

    private final String label;

    HorseStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
