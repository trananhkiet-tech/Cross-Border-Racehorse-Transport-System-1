package com.example.racetransport.health.entity;

/**
 * ObservationSeverity
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum ObservationSeverity {
    /** Bình thường */
    NORMAL("Bình thường"),
    /** Cần chú ý */
    ATTENTION("Cần chú ý"),
    /** Khẩn cấp */
    URGENT("Khẩn cấp");

    private final String label;

    ObservationSeverity(String label) { this.label = label; }

    public String getLabel() { return label; }
}
