package com.example.api_sell_clothes_v1.DTO.Brands;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandUpdateDTO {
    private String name;
    private String logoUrl;
    private String description;
    private Boolean status;
}
