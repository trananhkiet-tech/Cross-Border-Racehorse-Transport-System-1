package com.example.racetransport.trip.entity;

/**
 * TripStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum TripStatus {
    /** Đã lên lịch */
    SCHEDULED("Đã lên lịch"),
    /** Đang chuẩn bị */
    PREPARING("Đang chuẩn bị"),
    /** Sẵn sàng khởi hành */
    READY_FOR_DEPARTURE("Sẵn sàng khởi hành"),
    /** Đang vận chuyển */
    IN_TRANSIT("Đang vận chuyển"),
    /** Tại điểm dừng */
    AT_CHECKPOINT("Tại điểm dừng"),
    /** Tại cửa khẩu */
    AT_BORDER("Tại cửa khẩu"),
    /** Đang thông quan */
    CUSTOMS_CLEARANCE("Đang thông quan"),
    /** Đang kiểm dịch */
    QUARANTINE("Đang kiểm dịch"),
    /** Bị trễ / tạm dừng */
    DELAYED("Bị trễ / tạm dừng"),
    /** Đã đến nơi */
    ARRIVED("Đã đến nơi"),
    /** Chờ bàn giao */
    HANDOVER_PENDING("Chờ bàn giao"),
    /** Đã giao */
    DELIVERED("Đã giao"),
    /** Đã hủy */
    CANCELLED("Đã hủy");

    private final String label;

    TripStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
