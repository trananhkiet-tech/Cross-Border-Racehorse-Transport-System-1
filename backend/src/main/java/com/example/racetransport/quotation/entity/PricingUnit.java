package com.example.racetransport.quotation.entity;

/**
 * PricingUnit
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum PricingUnit {
    /** Theo km */
    PER_KM("Theo km"),
    /** Theo con ngựa */
    PER_HORSE("Theo con ngựa"),
    /** Theo chuyến */
    PER_TRIP("Theo chuyến"),
    /** Theo ngựa / ngày */
    PER_HORSE_DAY("Theo ngựa / ngày"),
    /** % giá trị khai báo (phí bảo hiểm) */
    PERCENT_OF_DECLARED_VALUE("% giá trị khai báo (phí bảo hiểm)");

    private final String label;

    PricingUnit(String label) { this.label = label; }

    public String getLabel() { return label; }
}
