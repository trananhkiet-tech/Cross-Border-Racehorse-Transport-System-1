package com.example.racetransport.transportationrequest.entity;

/**
 * MovementType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum MovementType {
    /** Tạm thời */
    TEMPORARY("Tạm thời"),
    /** Vĩnh viễn */
    PERMANENT("Vĩnh viễn");

    private final String label;

    MovementType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
