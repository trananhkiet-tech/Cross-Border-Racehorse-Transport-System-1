package com.example.racetransport.health.entity;

/**
 * StressLevel
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum StressLevel {
    /** Thấp */
    LOW("Thấp"),
    /** Trung bình */
    MEDIUM("Trung bình"),
    /** Cao */
    HIGH("Cao");

    private final String label;

    StressLevel(String label) { this.label = label; }

    public String getLabel() { return label; }
}
