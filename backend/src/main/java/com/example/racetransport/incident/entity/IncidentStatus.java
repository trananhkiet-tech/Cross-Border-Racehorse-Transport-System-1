package com.example.racetransport.incident.entity;

/**
 * IncidentStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum IncidentStatus {
    /** Mới mở */
    OPEN("Mới mở"),
    /** Đang xem xét */
    UNDER_REVIEW("Đang xem xét"),
    /** Cần hành động */
    ACTION_REQUIRED("Cần hành động"),
    /** Đang theo dõi */
    MONITORING("Đang theo dõi"),
    /** Đã xử lý */
    RESOLVED("Đã xử lý"),
    /** Đã đóng */
    CLOSED("Đã đóng");

    private final String label;

    IncidentStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
