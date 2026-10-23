package com.example.racetransport.horse.entity;

/**
 * HorseSex
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum HorseSex {
    /** Ngựa đực giống */
    STALLION("Ngựa đực giống"),
    /** Ngựa cái */
    MARE("Ngựa cái"),
    /** Ngựa thiến */
    GELDING("Ngựa thiến"),
    /** Ngựa đực non */
    COLT("Ngựa đực non"),
    /** Ngựa cái non */
    FILLY("Ngựa cái non");

    private final String label;

    HorseSex(String label) { this.label = label; }

    public String getLabel() { return label; }
}
