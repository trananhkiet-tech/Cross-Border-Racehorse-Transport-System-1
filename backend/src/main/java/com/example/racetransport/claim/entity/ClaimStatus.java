package com.example.racetransport.claim.entity;

/**
 * ClaimStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum ClaimStatus {
    /** Đã nộp */
    SUBMITTED("Đã nộp"),
    /** Đang thẩm định */
    UNDER_REVIEW("Đang thẩm định"),
    /** Cần bổ sung bằng chứng */
    EVIDENCE_REQUIRED("Cần bổ sung bằng chứng"),
    /** Được duyệt */
    APPROVED("Được duyệt"),
    /** Duyệt một phần */
    PARTIALLY_APPROVED("Duyệt một phần"),
    /** Bị từ chối */
    REJECTED("Bị từ chối"),
    /** Đã chi trả */
    PAID("Đã chi trả"),
    /** Đã đóng */
    CLOSED("Đã đóng");

    private final String label;

    ClaimStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
