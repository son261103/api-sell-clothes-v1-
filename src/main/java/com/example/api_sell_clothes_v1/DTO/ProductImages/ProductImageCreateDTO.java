package com.example.api_sell_clothes_v1.DTO.ProductImages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageCreateDTO {
    private Long productId;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
}
