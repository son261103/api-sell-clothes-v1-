package com.example.api_sell_clothes_v1.DTO.Categories;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryHierarchyDTO {
    private CategoryResponseDTO parent;
    private List<CategoryResponseDTO> subCategories;
    private int totalSubCategories;
    private int activeSubCategories;
    private int inactiveSubCategories;
}