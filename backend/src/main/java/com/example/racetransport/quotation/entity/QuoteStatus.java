package com.example.racetransport.quotation.entity;

/**
 * QuoteStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum QuoteStatus {
    /** Ước tính */
    ESTIMATED("Ước tính"),
    /** Nháp */
    DRAFT("Nháp"),
    /** Đã gửi khách */
    SENT("Đã gửi khách"),
    /** Đã chấp nhận */
    ACCEPTED("Đã chấp nhận"),
    /** Đã từ chối */
    REJECTED("Đã từ chối"),
    /** Hết hạn */
    EXPIRED("Hết hạn"),
    /** Đã thay thế bởi phiên bản mới */
    SUPERSEDED("Đã thay thế bởi phiên bản mới");

    private final String label;

    QuoteStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
