package com.example.api_sell_clothes_v1.DTO.Orders;

import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private Long orderId;
    private String orderCode;
    private Order.OrderStatus status;
    private String statusDescription;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount; // Tương đương totalAmount, đồng bộ với frontend
    private BigDecimal shippingFee;
    private String shippingMethodName;
    private int totalItems;
    private int itemCount; // Tương đương totalItems, đồng bộ với frontend
    private LocalDateTime createdAt;
    private String userName;
    private String userEmail;
    private Long userId;
    private Payment.PaymentStatus paymentStatus;

    // Thông tin về giảm giá
    private BigDecimal subtotalBeforeDiscount;
    private BigDecimal totalDiscount;
    private boolean hasCoupon;
}