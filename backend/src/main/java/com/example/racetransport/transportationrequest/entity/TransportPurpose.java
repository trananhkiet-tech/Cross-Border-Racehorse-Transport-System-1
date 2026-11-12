package com.example.racetransport.transportationrequest.entity;

/**
 * TransportPurpose
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum TransportPurpose {
    /** Đua */
    RACING("Đua"),
    /** Thi đấu */
    COMPETITION("Thi đấu"),
    /** Huấn luyện */
    TRAINING("Huấn luyện"),
    /** Phối giống */
    BREEDING("Phối giống"),
    /** Chuyển nơi ở */
    RELOCATION("Chuyển nơi ở"),
    /** Khác */
    OTHER("Khác");

    private final String label;

    TransportPurpose(String label) { this.label = label; }

    public String getLabel() { return label; }
}
