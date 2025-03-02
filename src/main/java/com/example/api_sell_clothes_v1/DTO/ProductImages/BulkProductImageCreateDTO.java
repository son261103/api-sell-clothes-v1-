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
public class BulkProductImageCreateDTO {
    private Long productId;
    private List<ImageDetail> images;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDetail {
        private String imageUrl;
        private Boolean isPrimary;
        private Integer displayOrder;
    }
}
