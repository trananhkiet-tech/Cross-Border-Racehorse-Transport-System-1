package com.example.racetransport.health.entity;

/**
 * IntakeLevel
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum IntakeLevel {
    /** Bình thường */
    NORMAL("Bình thường"),
    /** Giảm */
    REDUCED("Giảm"),
    /** Kém */
    POOR("Kém"),
    /** Không ăn / uống */
    NONE("Không ăn / uống");

    private final String label;

    IntakeLevel(String label) { this.label = label; }

    public String getLabel() { return label; }
}
