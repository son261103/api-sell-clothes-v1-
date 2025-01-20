package com.example.api_sell_clothes_v1.DTO.ProductVariant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantUpdateDTO {
    private String size;
    private String color;
    private String sku;
    private Integer stockQuantity;
    private String imageUrl;
    private Boolean status;
}