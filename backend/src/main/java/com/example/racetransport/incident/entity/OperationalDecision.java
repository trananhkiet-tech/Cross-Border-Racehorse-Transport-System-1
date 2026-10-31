package com.example.racetransport.incident.entity;

/**
 * OperationalDecision
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum OperationalDecision {
    /** Tiếp tục hành trình */
    CONTINUE("Tiếp tục hành trình"),
    /** Tiếp tục & theo dõi */
    MONITOR("Tiếp tục & theo dõi"),
    /** Dừng tại chỗ */
    STOP("Dừng tại chỗ"),
    /** Đổi tuyến */
    REROUTE("Đổi tuyến");

    private final String label;

    OperationalDecision(String label) { this.label = label; }

    public String getLabel() { return label; }
}
