package com.example.api_sell_clothes_v1.DTO.ReviewComments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a new review comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommentCreateDTO {
    private Long reviewId;
    private Long userId;
    private String content;
}