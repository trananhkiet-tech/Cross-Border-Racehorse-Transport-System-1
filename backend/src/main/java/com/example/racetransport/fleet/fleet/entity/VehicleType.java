package com.example.racetransport.fleet.entity;

/**
 * VehicleType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum VehicleType {
    /** Xe tải chở ngựa */
    HORSE_TRUCK("Xe tải chở ngựa"),
    /** Rơ-moóc chở ngựa */
    HORSE_TRAILER("Rơ-moóc chở ngựa"),
    /** Chuồng bay (air stall) */
    AIR_STALL("Chuồng bay (air stall)"),
    /** Xe hỗ trợ */
    SUPPORT_VEHICLE("Xe hỗ trợ");

    private final String label;

    VehicleType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
