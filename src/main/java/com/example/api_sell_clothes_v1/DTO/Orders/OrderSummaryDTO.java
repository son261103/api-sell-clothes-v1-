package com.example.api_sell_clothes_v1.DTO.Orders;

import com.example.api_sell_clothes_v1.Entity.Order.OrderStatus;
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
    private OrderStatus status;
    private String statusDescription;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private LocalDateTime createdAt;
}