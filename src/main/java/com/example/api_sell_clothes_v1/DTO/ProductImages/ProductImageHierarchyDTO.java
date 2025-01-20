package com.example.api_sell_clothes_v1.DTO.ProductImages;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageHierarchyDTO {
    private List<ProductImageResponseDTO> images;
    private Long productId;
    private int totalImages;
    private int primaryImages;
    private int nonPrimaryImages;
}
