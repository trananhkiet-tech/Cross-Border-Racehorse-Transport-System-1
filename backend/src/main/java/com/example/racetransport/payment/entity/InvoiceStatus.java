package com.example.racetransport.payment.entity;

/**
 * InvoiceStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum InvoiceStatus {
    /** Đã phát hành */
    ISSUED("Đã phát hành"),
    /** Đã thanh toán một phần */
    PARTIALLY_PAID("Đã thanh toán một phần"),
    /** Đã thanh toán */
    PAID("Đã thanh toán"),
    /** Quá hạn */
    OVERDUE("Quá hạn"),
    /** Đã hủy */
    CANCELLED("Đã hủy");

    private final String label;

    InvoiceStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
