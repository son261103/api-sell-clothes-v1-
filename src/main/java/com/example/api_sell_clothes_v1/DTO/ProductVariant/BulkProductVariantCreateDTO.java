package com.example.api_sell_clothes_v1.DTO.ProductVariant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkProductVariantCreateDTO {
    private Long productId;
    private List<VariantDetail> variants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantDetail {
        private String size;
        private String color;
        private Integer stockQuantity;
        private String sku;  // Optional - sẽ được tự động generate nếu không cung cấp
    }
}