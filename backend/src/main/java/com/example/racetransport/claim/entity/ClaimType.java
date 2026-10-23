package com.example.racetransport.claim.entity;

/**
 * ClaimType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum ClaimType {
    /** Chi phí điều trị chấn thương */
    INJURY_MEDICAL_EXPENSE("Chi phí điều trị chấn thương"),
    /** Ngựa chết */
    MORTALITY("Ngựa chết"),
    /** An tử nhân đạo */
    HUMANE_EUTHANASIA("An tử nhân đạo"),
    /** Mất cắp / thất lạc */
    THEFT_OR_LOSS("Mất cắp / thất lạc"),
    /** Hư hỏng tài sản */
    PROPERTY_DAMAGE("Hư hỏng tài sản"),
    /** Chậm trễ dịch vụ */
    SERVICE_DELAY("Chậm trễ dịch vụ"),
    /** Tổn thất trực tiếp khác */
    OTHER_DIRECT_LOSS("Tổn thất trực tiếp khác");

    private final String label;

    ClaimType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
