package com.example.racetransport.user.entity;

/**
 * CustomerType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum CustomerType {
    /** Cá nhân */
    INDIVIDUAL("Cá nhân"),
    /** Trại ngựa / chuồng đua */
    STABLE("Trại ngựa / chuồng đua"),
    /** Doanh nghiệp */
    COMPANY("Doanh nghiệp"),
    /** Đại lý */
    AGENT("Đại lý");

    private final String label;

    CustomerType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
