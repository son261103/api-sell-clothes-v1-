package com.example.api_sell_clothes_v1.DTO.Orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsDTO {
    private long totalOrders;
    private long pendingOrders;
    private long processingOrders;
    private long shippingOrders;
    private long completedOrders;
    private long cancelledOrders;
}