package com.example.racetransport.health.entity;

/**
 * MovementCondition
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum MovementCondition {
    /** Bình thường */
    NORMAL("Bình thường"),
    /** Cứng / khó chịu */
    STIFF("Cứng / khó chịu"),
    /** Đi khập khiễng */
    LAME("Đi khập khiễng"),
    /** Không chịu di chuyển */
    REFUSES_TO_MOVE("Không chịu di chuyển");

    private final String label;

    MovementCondition(String label) { this.label = label; }

    public String getLabel() { return label; }
}
