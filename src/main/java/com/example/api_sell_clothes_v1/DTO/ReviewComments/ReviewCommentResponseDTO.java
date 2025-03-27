package com.example.api_sell_clothes_v1.DTO.ReviewComments;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommentResponseDTO {
    private Long commentId;
    private Long reviewId;
    private Long userId;
    private String username;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isCurrentUser;
}
