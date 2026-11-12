package com.example.racetransport.health.entity;

/**
 * HorseCondition
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum HorseCondition {
    /** Tốt */
    GOOD("Tốt"),
    /** Khá */
    FAIR("Khá"),
    /** Kém */
    POOR("Kém"),
    /** Bị thương */
    INJURED("Bị thương");

    private final String label;

    HorseCondition(String label) { this.label = label; }

    public String getLabel() { return label; }
}
