package com.example.api_sell_clothes_v1.DTO.ProductReviews;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewUpdateDTO {
    private Integer rating;
    private String comment;
}
