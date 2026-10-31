package com.example.racetransport.incident.entity;

/**
 * IncidentSeverity
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum IncidentSeverity {
    /** Thấp */
    LOW("Thấp"),
    /** Trung bình */
    MEDIUM("Trung bình"),
    /** Cao */
    HIGH("Cao"),
    /** Nghiêm trọng */
    CRITICAL("Nghiêm trọng");

    private final String label;

    IncidentSeverity(String label) { this.label = label; }

    public String getLabel() { return label; }
}
