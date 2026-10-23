package com.example.racetransport.health.entity;

/**
 * HorseBehavior
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum HorseBehavior {
    /** Bình tĩnh */
    CALM("Bình tĩnh"),
    /** Tỉnh táo */
    ALERT("Tỉnh táo"),
    /** Bồn chồn */
    RESTLESS("Bồn chồn"),
    /** Kích động */
    AGITATED("Kích động"),
    /** Lờ đờ */
    LETHARGIC("Lờ đờ");

    private final String label;

    HorseBehavior(String label) { this.label = label; }

    public String getLabel() { return label; }
}
