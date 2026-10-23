package com.example.racetransport.quotation.entity;

/**
 * CostCategory
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum CostCategory {
    /** Vận chuyển đường bộ */
    GROUND_TRANSPORT("Vận chuyển đường bộ"),
    /** Vận chuyển hàng không */
    AIR_TRANSPORT("Vận chuyển hàng không"),
    /** Bốc xếp / xử lý */
    HANDLING("Bốc xếp / xử lý"),
    /** Thú y / xét nghiệm */
    VETERINARY_TESTING("Thú y / xét nghiệm"),
    /** Giấy tờ */
    DOCUMENTS("Giấy tờ"),
    /** Hải quan */
    CUSTOMS("Hải quan"),
    /** Kiểm dịch / lưu chuồng */
    QUARANTINE_STABLING("Kiểm dịch / lưu chuồng"),
    /** Áp tải */
    ESCORT("Áp tải"),
    /** Bảo hiểm */
    INSURANCE("Bảo hiểm"),
    /** Phụ phí */
    SURCHARGE("Phụ phí"),
    /** Giảm giá */
    DISCOUNT("Giảm giá");

    private final String label;

    CostCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}
