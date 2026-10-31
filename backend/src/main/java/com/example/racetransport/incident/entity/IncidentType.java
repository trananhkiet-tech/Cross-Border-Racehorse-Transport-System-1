package com.example.racetransport.incident.entity;

/**
 * IncidentType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum IncidentType {
    /** Sức khỏe ngựa */
    HORSE_HEALTH("Sức khỏe ngựa"),
    /** Ngựa bị thương */
    HORSE_INJURY("Ngựa bị thương"),
    /** Sự cố phương tiện */
    VEHICLE_BREAKDOWN("Sự cố phương tiện"),
    /** Giao thông / kẹt xe */
    TRAFFIC_DELAY("Giao thông / kẹt xe"),
    /** Chậm trễ cửa khẩu */
    BORDER_DELAY("Chậm trễ cửa khẩu"),
    /** Vấn đề giấy tờ / tuân thủ */
    DOCUMENT_ISSUE("Vấn đề giấy tờ / tuân thủ"),
    /** Thời tiết */
    WEATHER("Thời tiết"),
    /** An ninh */
    SECURITY("An ninh"),
    /** Khác */
    OTHER("Khác");

    private final String label;

    IncidentType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
