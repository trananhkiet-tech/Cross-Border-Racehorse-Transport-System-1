package com.example.racetransport.transportationrequest.entity;

/**
 * TransportMode
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum TransportMode {
    /** Đường bộ */
    ROAD("Đường bộ"),
    /** Hàng không */
    AIR("Hàng không"),
    /** Kết hợp bộ + hàng không */
    ROAD_AIR("Kết hợp bộ + hàng không");

    private final String label;

    TransportMode(String label) { this.label = label; }

    public String getLabel() { return label; }
}
