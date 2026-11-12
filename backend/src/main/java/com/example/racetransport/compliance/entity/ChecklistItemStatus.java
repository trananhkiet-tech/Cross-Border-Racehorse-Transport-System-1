package com.example.racetransport.compliance.entity;

/**
 * ChecklistItemStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum ChecklistItemStatus {
    /** Chưa nộp */
    MISSING("Chưa nộp"),
    /** Đã nộp – chờ rà soát */
    SUBMITTED("Đã nộp – chờ rà soát"),
    /** Đạt */
    APPROVED("Đạt"),
    /** Không đạt */
    REJECTED("Không đạt"),
    /** Cần cập nhật */
    NEEDS_UPDATE("Cần cập nhật"),
    /** Hết hạn */
    EXPIRED("Hết hạn"),
    /** Miễn trừ */
    WAIVED("Miễn trừ");

    private final String label;

    ChecklistItemStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
