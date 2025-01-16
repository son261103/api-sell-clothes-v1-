package com.example.api_sell_clothes_v1.DTO.Categories;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateDTO {
    private Long categoryId;
    private String name;
    private Long parentId;
    private String description;
    private String slug;
    private Boolean status;
}
