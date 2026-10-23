package com.example.racetransport.payment.entity;

/**
 * PaymentStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum PaymentStatus {
    /** Thành công */
    SUCCEEDED("Thành công"),
    /** Thất bại */
    FAILED("Thất bại");

    private final String label;

    PaymentStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
