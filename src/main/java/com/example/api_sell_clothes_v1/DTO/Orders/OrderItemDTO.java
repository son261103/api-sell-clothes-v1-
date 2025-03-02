package com.example.api_sell_clothes_v1.DTO.Orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long orderId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String productImage;
    private String sku;
    private String size;
    private String color;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}