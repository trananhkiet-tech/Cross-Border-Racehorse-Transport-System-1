package com.example.racetransport.incident.entity;

/**
 * IncidentActionType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum IncidentActionType {
    /** Ghi chú */
    NOTE("Ghi chú"),
    /** Đã liên hệ thú y */
    VET_CONTACTED("Đã liên hệ thú y"),
    /** Ghi nhận tư vấn thú y */
    VET_ADVICE_RECORDED("Ghi nhận tư vấn thú y"),
    /** Quyết định vận hành */
    DECISION("Quyết định vận hành"),
    /** Đổi trạng thái */
    STATUS_CHANGE("Đổi trạng thái"),
    /** Báo cáo cấp trên */
    ESCALATION("Báo cáo cấp trên"),
    /** Xử lý tại hiện trường */
    FIELD_ACTION("Xử lý tại hiện trường");

    private final String label;

    IncidentActionType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
