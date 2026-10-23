package com.example.racetransport.claim.entity;

/**
 * SettlementStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum SettlementStatus {
    /** Đã chi trả (mô phỏng) */
    PAID("Đã chi trả (mô phỏng)"),
    /** Thất bại */
    FAILED("Thất bại");

    private final String label;

    SettlementStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
