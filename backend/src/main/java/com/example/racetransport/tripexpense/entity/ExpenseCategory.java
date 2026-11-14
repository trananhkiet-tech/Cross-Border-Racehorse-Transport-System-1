package com.example.racetransport.tripexpense.entity;

public enum ExpenseCategory {
    FUEL("Nhiên liệu"),
    TOLL("Phí cầu đường"),
    FEED("Thức ăn / nước"),
    VET("Thú y"),
    LODGING("Lưu trú"),
    OTHER("Khác");

    private final String label;

    ExpenseCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}
