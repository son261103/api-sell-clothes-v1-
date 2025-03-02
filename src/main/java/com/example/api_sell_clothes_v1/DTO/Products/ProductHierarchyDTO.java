package com.example.api_sell_clothes_v1.DTO.Products;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductHierarchyDTO {
    private List<ProductResponseDTO> products;
    private int totalProducts;
    private int activeProducts;
    private int inactiveProducts;
}