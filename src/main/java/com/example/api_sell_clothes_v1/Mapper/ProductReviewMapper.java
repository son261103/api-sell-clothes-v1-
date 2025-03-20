package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.ProductReviews;
import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductReviewMapper implements EntityMapper<ProductReviews, ProductReviewResponseDTO> {

    @Override
    public ProductReviews toEntity(ProductReviewResponseDTO dto) {
        if (dto == null) {
            return null;
        }

        Products product = new Products();
        product.setProductId(dto.getProductId());

        Users user = new Users();
        user.setUserId(dto.getUserId());

        return ProductReviews.builder()
                .reviewId(dto.getReviewId())
                .product(product)
                .user(user)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    @Override
    public ProductReviewResponseDTO toDto(ProductReviews entity) {
        if (entity == null) {
            return null;
        }

        return ProductReviewResponseDTO.builder()
                .reviewId(entity.getReviewId())
                .productId(entity.getProduct().getProductId())
                .productName(entity.getProduct().getName())
                .userId(entity.getUser().getUserId())
                .username(entity.getUser().getUsername())
                .userAvatar(entity.getUser().getAvatar())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<ProductReviews> toEntity(List<ProductReviewResponseDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<ProductReviewResponseDTO> toDto(List<ProductReviews> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProductReviews createDtoToEntity(ProductReviewCreateDTO dto, Products product, Users user) {
        if (dto == null) {
            return null;
        }

        return ProductReviews.builder()
                .product(product)
                .user(user)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();
    }

    public void updateEntityFromDto(ProductReviewUpdateDTO dto, ProductReviews entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getRating() != null) {
            entity.setRating(dto.getRating());
        }

        if (dto.getComment() != null) {
            entity.setComment(dto.getComment());
        }
    }
}