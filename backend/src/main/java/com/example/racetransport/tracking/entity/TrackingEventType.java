package com.example.racetransport.tracking.entity;

/**
 * TrackingEventType
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum TrackingEventType {
    /** Khởi hành */
    DEPARTED("Khởi hành"),
    /** Đến điểm dừng */
    CHECKPOINT_ARRIVED("Đến điểm dừng"),
    /** Rời điểm dừng */
    CHECKPOINT_DEPARTED("Rời điểm dừng"),
    /** Cập nhật vị trí */
    LOCATION_UPDATE("Cập nhật vị trí"),
    /** Đổi trạng thái */
    STATUS_CHANGE("Đổi trạng thái"),
    /** Báo trễ */
    DELAY("Báo trễ"),
    /** Đến nơi */
    ARRIVED("Đến nơi"),
    /** Ghi chú */
    NOTE("Ghi chú");

    private final String label;

    TrackingEventType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
