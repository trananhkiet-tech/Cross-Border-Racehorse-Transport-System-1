package com.example.racetransport.staffleave.entity;

public enum LeaveStatus {
    ACTIVE("Có hiệu lực"),
    CANCELLED("Đã hủy");

    private final String label;

    LeaveStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
