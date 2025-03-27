package com.example.api_sell_clothes_v1.DTO.ProductReviews;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponseDTO {
    private Long reviewId;
    private Long productId;
    private String productName;
    private Long userId;
    private String username;
    private String userAvatar;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}