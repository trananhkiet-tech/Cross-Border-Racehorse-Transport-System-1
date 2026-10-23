package com.example.racetransport.document.entity;

/**
 * AttachmentRefType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum AttachmentRefType {
    /** Quan sát sức khỏe */
    HEALTH_OBSERVATION("Quan sát sức khỏe"),
    /** Sự cố */
    INCIDENT("Sự cố"),
    /** Bàn giao */
    HANDOVER("Bàn giao"),
    /** Kiểm tra khi đến */
    ARRIVAL_CHECK("Kiểm tra khi đến"),
    /** Tư vấn thú y */
    VET_CONSULTATION("Tư vấn thú y"),
    /** Sự kiện hành trình */
    TRACKING_EVENT("Sự kiện hành trình");

    private final String label;

    AttachmentRefType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
