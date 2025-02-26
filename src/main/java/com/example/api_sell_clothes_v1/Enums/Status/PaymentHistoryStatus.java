package com.example.api_sell_clothes_v1.Enums.Status;

public enum PaymentHistoryStatus {
    PENDING("Chờ thanh toán"),
    COMPLETED("Đã hoàn thành"),
    FAILED("Thất bại"),
    REFUNDED("Đã hoàn tiền");

    private final String description;

    PaymentHistoryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}