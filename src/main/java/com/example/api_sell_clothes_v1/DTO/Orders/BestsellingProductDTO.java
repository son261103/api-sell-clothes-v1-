package com.example.api_sell_clothes_v1.DTO.Orders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestsellingProductDTO {
    private Long productId;
    private String productName;
    private String productImage;
    private Integer totalQuantitySold;
}