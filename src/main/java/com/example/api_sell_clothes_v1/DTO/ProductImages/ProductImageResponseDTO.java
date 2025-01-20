package com.example.api_sell_clothes_v1.DTO.ProductImages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageResponseDTO {
    private Long imageId;
    private String imageUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
}
