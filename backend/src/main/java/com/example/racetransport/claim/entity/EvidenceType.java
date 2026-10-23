package com.example.racetransport.claim.entity;

/**
 * EvidenceType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum EvidenceType {
    /** Ảnh */
    PHOTO("Ảnh"),
    /** Báo cáo thú y */
    VET_REPORT("Báo cáo thú y"),
    /** Hóa đơn / chứng từ chi phí */
    RECEIPT("Hóa đơn / chứng từ chi phí"),
    /** Biên bản */
    OFFICIAL_REPORT("Biên bản"),
    /** Khác */
    OTHER("Khác");

    private final String label;

    EvidenceType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
