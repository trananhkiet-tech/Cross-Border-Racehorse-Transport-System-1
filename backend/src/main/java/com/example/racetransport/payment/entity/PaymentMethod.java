package com.example.racetransport.payment.entity;

/**
 * PaymentMethod
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum PaymentMethod {
    /** Thẻ (mô phỏng) */
    SIMULATED_CARD("Thẻ (mô phỏng)"),
    /** Chuyển khoản (mô phỏng) */
    SIMULATED_BANK_TRANSFER("Chuyển khoản (mô phỏng)"),
    /** Ví điện tử (mô phỏng) */
    SIMULATED_EWALLET("Ví điện tử (mô phỏng)");

    private final String label;

    PaymentMethod(String label) { this.label = label; }

    public String getLabel() { return label; }
}
