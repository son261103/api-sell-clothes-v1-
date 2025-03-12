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
public class ProductExcelTemplateResponseDTO {
    private String version;
    private String lastUpdated;
    private List<String> supportedFeatures;
    private boolean includesCategories;
    private boolean includesBrands;
    private boolean includesVariants;
    private String templateUrl;
}