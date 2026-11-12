package com.example.racetransport.route.entity;

/**
 * StopType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum StopType {
    /** Điểm nhận ngựa */
    PICKUP("Điểm nhận ngựa"),
    /** Điểm nghỉ */
    REST_STOP("Điểm nghỉ"),
    /** Điểm kiểm tra */
    CHECKPOINT("Điểm kiểm tra"),
    /** Sân bay */
    AIRPORT("Sân bay"),
    /** Cửa khẩu */
    BORDER("Cửa khẩu"),
    /** Hải quan */
    CUSTOMS("Hải quan"),
    /** Kiểm dịch / cách ly */
    QUARANTINE("Kiểm dịch / cách ly"),
    /** Chuồng trung chuyển */
    STABLE("Chuồng trung chuyển"),
    /** Điểm đến */
    DESTINATION("Điểm đến");

    private final String label;

    StopType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
