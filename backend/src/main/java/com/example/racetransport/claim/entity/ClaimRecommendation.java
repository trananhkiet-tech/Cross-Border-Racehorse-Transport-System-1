package com.example.racetransport.claim.entity;

/**
 * ClaimRecommendation
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum ClaimRecommendation {
    /** Đề xuất duyệt */
    APPROVE("Đề xuất duyệt"),
    /** Đề xuất duyệt một phần */
    PARTIALLY_APPROVE("Đề xuất duyệt một phần"),
    /** Đề xuất từ chối */
    REJECT("Đề xuất từ chối"),
    /** Yêu cầu bổ sung bằng chứng */
    REQUEST_EVIDENCE("Yêu cầu bổ sung bằng chứng");

    private final String label;

    ClaimRecommendation(String label) { this.label = label; }

    public String getLabel() { return label; }
}
