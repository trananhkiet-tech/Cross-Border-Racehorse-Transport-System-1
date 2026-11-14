package com.example.racetransport.tripexpense.entity;

public enum ExpenseStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối");

    private final String label;

    ExpenseStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
