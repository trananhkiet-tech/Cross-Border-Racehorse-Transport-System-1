package com.example.racetransport.transportationrequest.entity;

/**
 * RequestStatus
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum RequestStatus {
    /** Bản nháp */
    DRAFT("Bản nháp"),
    /** Đã gửi */
    SUBMITTED("Đã gửi"),
    /** Đang rà soát tài liệu */
    UNDER_DOCUMENT_REVIEW("Đang rà soát tài liệu"),
    /** Cần bổ sung thông tin */
    INFORMATION_REQUIRED("Cần bổ sung thông tin"),
    /** Đang kiểm tra tuân thủ */
    COMPLIANCE_REVIEW("Đang kiểm tra tuân thủ"),
    /** Chờ quản lý phê duyệt */
    PENDING_MANAGER_APPROVAL("Chờ quản lý phê duyệt"),
    /** Đã phê duyệt */
    APPROVED("Đã phê duyệt"),
    /** Bị từ chối */
    REJECTED("Bị từ chối"),
    /** Đã gửi báo giá */
    QUOTE_SENT("Đã gửi báo giá"),
    /** Đã chấp nhận báo giá */
    QUOTE_ACCEPTED("Đã chấp nhận báo giá"),
    /** Đã từ chối báo giá */
    QUOTE_REJECTED("Đã từ chối báo giá"),
    /** Đã hủy */
    CANCELLED("Đã hủy"),
    /** Sẵn sàng lập chuyến */
    READY_FOR_PLANNING("Sẵn sàng lập chuyến"),
    /** Đã lập chuyến */
    TRIP_PLANNED("Đã lập chuyến"),
    /** Hoàn tất */
    COMPLETED("Hoàn tất");

    private final String label;

    RequestStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
