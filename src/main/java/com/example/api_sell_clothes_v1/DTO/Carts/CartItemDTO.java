package com.example.api_sell_clothes_v1.DTO.Carts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long itemId;
    private Long cartId;
    private Long variantId;
    private Long productId;
    private String productName;
    private String sku;
    private String size;
    private String color;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
    private Integer stockQuantity;
    private Boolean isSelected;
}