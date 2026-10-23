package com.example.racetransport.user.entity;

/**
 * Đúng 5 role nghiệp vụ, không có VETERINARIAN
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum Role {
    /** Khách hàng */
    CUSTOMER("Khách hàng"),
    /** Quản lý Logistics */
    LOGISTICS_MANAGER("Quản lý Logistics"),
    /** Chuyên viên vận chuyển */
    TRANSPORT_SPECIALIST("Chuyên viên vận chuyển"),
    /** Điều phối đội xe & tuyến */
    FLEET_ROUTE_COORDINATOR("Điều phối đội xe & tuyến"),
    /** Tài xế / Người áp tải */
    DRIVER_ESCORT("Tài xế / Người áp tải");

    private final String label;

    Role(String label) { this.label = label; }

    public String getLabel() { return label; }
}
