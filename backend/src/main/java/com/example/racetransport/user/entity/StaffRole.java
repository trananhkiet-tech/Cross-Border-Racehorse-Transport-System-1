package com.example.racetransport.user.entity;

/**
 * Vai trò chuyên môn của nhân viên DRIVER_ESCORT và vai trò trong phân công chuyến
 * Nhãn tiếng Việt dùng cho thông báo/hiển thị phía server; frontend có bản nhãn riêng (src/types/enums.ts).
 */
public enum StaffRole {
    /** Tài xế */
    DRIVER("Tài xế"),
    /** Người áp tải */
    ESCORT("Người áp tải"),
    /** Trưởng nhóm */
    TEAM_LEADER("Trưởng nhóm");

    private final String label;

    StaffRole(String label) { this.label = label; }

    public String getLabel() { return label; }
}
