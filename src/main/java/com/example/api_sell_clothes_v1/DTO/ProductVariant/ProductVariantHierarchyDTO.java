package com.example.api_sell_clothes_v1.DTO.ProductVariant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantHierarchyDTO {
    private List<ProductVariantResponseDTO> variants;
    private Long productId;
    private int totalVariants;
    private int activeVariants;
    private int inactiveVariants;
    private int totalStock;
    private Map<String, Integer> stockBySize;
    private Map<String, Integer> stockByColor;
}