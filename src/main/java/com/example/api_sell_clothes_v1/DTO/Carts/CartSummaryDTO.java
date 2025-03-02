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
public class CartSummaryDTO {
    private Long cartId;
    private Integer totalItems;
    private BigDecimal totalPrice;
    private Integer selectedItems;
    private BigDecimal selectedTotalPrice;
}