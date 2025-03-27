package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.ProductReviews;
import com.example.api_sell_clothes_v1.Entity.ReviewComments;
import com.example.api_sell_clothes_v1.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewCommentMapper implements EntityMapper<ReviewComments, ReviewCommentResponseDTO> {

    @Override
    public ReviewComments toEntity(ReviewCommentResponseDTO dto) {
        if (dto == null) {
            return null;
        }

        ProductReviews review = new ProductReviews();
        review.setReviewId(dto.getReviewId());

        Users user = new Users();
        user.setUserId(dto.getUserId());

        return ReviewComments.builder()
                .commentId(dto.getCommentId())
                .review(review)
                .user(user)
                .content(dto.getContent())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public ReviewCommentResponseDTO toDto(ReviewComments entity) {
        return toDto(entity, null);
    }

    public ReviewCommentResponseDTO toDto(ReviewComments entity, Long currentUserId) {
        if (entity == null) {
            return null;
        }

        boolean isCurrentUser = currentUserId != null &&
                Objects.equals(currentUserId, entity.getUser().getUserId());

        return ReviewCommentResponseDTO.builder()
                .commentId(entity.getCommentId())
                .reviewId(entity.getReview().getReviewId())
                .userId(entity.getUser().getUserId())
                .username(entity.getUser().getUsername())
                .userAvatar(entity.getUser().getAvatar())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isCurrentUser(isCurrentUser)
                .build();
    }

    @Override
    public List<ReviewComments> toEntity(List<ReviewCommentResponseDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<ReviewCommentResponseDTO> toDto(List<ReviewComments> entityList) {
        return toDto(entityList, null);
    }

    public List<ReviewCommentResponseDTO> toDto(List<ReviewComments> entityList, Long currentUserId) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream()
                .map(entity -> toDto(entity, currentUserId))
                .collect(Collectors.toList());
    }

    public ReviewComments createDtoToEntity(ReviewCommentCreateDTO dto, ProductReviews review, Users user) {
        if (dto == null) {
            return null;
        }

        return ReviewComments.builder()
                .review(review)
                .user(user)
                .content(dto.getContent())
                .build();
    }

    public void updateEntityFromDto(ReviewCommentUpdateDTO dto, ReviewComments entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            entity.setContent(dto.getContent());
        }
    }
}