package com.example.racetransport.handover.entity;

/**
 * HandoverStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum HandoverStatus {
    /** Chờ xác nhận */
    PENDING_CONFIRMATION("Chờ xác nhận"),
    /** Đã xác nhận */
    CONFIRMED("Đã xác nhận"),
    /** Có khiếu nại */
    DISPUTED("Có khiếu nại");

    private final String label;

    HandoverStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
