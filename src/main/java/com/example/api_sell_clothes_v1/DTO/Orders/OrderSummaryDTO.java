package com.example.api_sell_clothes_v1.DTO.Orders;

import com.example.api_sell_clothes_v1.Entity.Order;
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
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private String shippingMethodName;
    private Order.OrderStatus status;
    private String statusDescription;
    private LocalDateTime createdAt;
    private int totalItems;
}