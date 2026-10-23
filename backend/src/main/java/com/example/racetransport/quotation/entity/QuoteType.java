package com.example.racetransport.quotation.entity;

/**
 * QuoteType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum QuoteType {
    /** Chi phí ước tính */
    ESTIMATE("Chi phí ước tính"),
    /** Báo giá cuối */
    FINAL("Báo giá cuối");

    private final String label;

    QuoteType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
