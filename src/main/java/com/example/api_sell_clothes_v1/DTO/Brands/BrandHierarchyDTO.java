package com.example.api_sell_clothes_v1.DTO.Brands;

import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandHierarchyDTO {
    private List<BrandResponseDTO> brands;
    private int totalBrands;
    private int activeBrands;
    private int inactiveBrands;
}
