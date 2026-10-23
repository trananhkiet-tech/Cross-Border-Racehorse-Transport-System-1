package com.example.racetransport.compliance.entity;

/**
 * ComplianceCategory
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum ComplianceCategory {
    /** Nhận dạng */
    IDENTIFICATION("Nhận dạng"),
    /** Tiêm phòng */
    VACCINATION("Tiêm phòng"),
    /** Sức khỏe */
    HEALTH("Sức khỏe"),
    /** Xét nghiệm */
    LABORATORY("Xét nghiệm"),
    /** Xuất / nhập khẩu */
    IMPORT_EXPORT("Xuất / nhập khẩu"),
    /** Kiểm dịch */
    QUARANTINE("Kiểm dịch"),
    /** Hải quan */
    CUSTOMS("Hải quan"),
    /** Bảo hiểm */
    INSURANCE("Bảo hiểm");

    private final String label;

    ComplianceCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}
