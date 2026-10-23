package com.example.racetransport.fleet.entity;

/**
 * VehicleStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum VehicleStatus {
    /** Sẵn sàng */
    AVAILABLE("Sẵn sàng"),
    /** Đang sử dụng */
    IN_USE("Đang sử dụng"),
    /** Đang bảo dưỡng */
    MAINTENANCE("Đang bảo dưỡng"),
    /** Không khả dụng */
    UNAVAILABLE("Không khả dụng"),
    /** Ngừng sử dụng */
    RETIRED("Ngừng sử dụng");

    private final String label;

    VehicleStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
