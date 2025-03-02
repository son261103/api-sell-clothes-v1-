package com.example.api_sell_clothes_v1.DTO.Products;

import com.example.api_sell_clothes_v1.DTO.Brands.BrandResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {
    private Long productId;
    private CategoryResponseDTO category;
    private BrandResponseDTO brand;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String thumbnail;
    private String slug;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}