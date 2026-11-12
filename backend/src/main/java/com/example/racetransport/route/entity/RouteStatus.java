package com.example.racetransport.route.entity;

/**
 * RouteStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum RouteStatus {
    /** Nháp */
    DRAFT("Nháp"),
    /** Đang dùng */
    ACTIVE("Đang dùng"),
    /** Ngừng dùng */
    INACTIVE("Ngừng dùng");

    private final String label;

    RouteStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
