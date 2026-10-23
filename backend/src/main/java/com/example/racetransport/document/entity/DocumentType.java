package com.example.racetransport.document.entity;

/**
 * DocumentType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum DocumentType {
    /** Hộ chiếu ngựa */
    HORSE_PASSPORT("Hộ chiếu ngựa"),
    /** Chứng nhận microchip / nhận dạng */
    MICROCHIP_CERTIFICATE("Chứng nhận microchip / nhận dạng"),
    /** Hồ sơ tiêm phòng */
    VACCINATION_RECORD("Hồ sơ tiêm phòng"),
    /** Giấy chứng nhận sức khỏe */
    HEALTH_CERTIFICATE("Giấy chứng nhận sức khỏe"),
    /** Kết quả xét nghiệm */
    LAB_TEST_REPORT("Kết quả xét nghiệm"),
    /** Giấy phép nhập khẩu */
    IMPORT_PERMIT("Giấy phép nhập khẩu"),
    /** Giấy phép xuất khẩu */
    EXPORT_PERMIT("Giấy phép xuất khẩu"),
    /** Chứng nhận kiểm dịch / cách ly */
    QUARANTINE_CERTIFICATE("Chứng nhận kiểm dịch / cách ly"),
    /** Tờ khai hải quan */
    CUSTOMS_DECLARATION("Tờ khai hải quan"),
    /** Giấy tờ sở hữu */
    OWNERSHIP_PROOF("Giấy tờ sở hữu"),
    /** Giấy chứng nhận bảo hiểm */
    INSURANCE_CERTIFICATE("Giấy chứng nhận bảo hiểm"),
    /** Báo cáo thú y bên ngoài */
    VET_REPORT("Báo cáo thú y bên ngoài"),
    /** Tài liệu khác */
    OTHER("Tài liệu khác");

    private final String label;

    DocumentType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
