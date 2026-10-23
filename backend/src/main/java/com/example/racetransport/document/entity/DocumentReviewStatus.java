package com.example.racetransport.document.entity;

/**
 * DocumentReviewStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum DocumentReviewStatus {
    /** Chờ rà soát */
    PENDING_REVIEW("Chờ rà soát"),
    /** Đã duyệt */
    APPROVED("Đã duyệt"),
    /** Bị từ chối */
    REJECTED("Bị từ chối"),
    /** Cần cập nhật */
    NEEDS_UPDATE("Cần cập nhật"),
    /** Hết hạn */
    EXPIRED("Hết hạn");

    private final String label;

    DocumentReviewStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
