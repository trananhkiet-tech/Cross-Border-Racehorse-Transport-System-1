package com.example.racetransport.notification.entity;

/**
 * NotificationType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum NotificationType {
    /** Thông tin */
    INFO("Thông tin"),
    /** Thành công */
    SUCCESS("Thành công"),
    /** Cảnh báo */
    WARNING("Cảnh báo"),
    /** Cần xử lý */
    ACTION_REQUIRED("Cần xử lý"),
    /** Khẩn cấp */
    CRITICAL("Khẩn cấp");

    private final String label;

    NotificationType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
