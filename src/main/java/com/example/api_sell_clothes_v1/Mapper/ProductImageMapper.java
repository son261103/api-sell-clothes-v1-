package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.ProductImages.ProductImageCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductImages.ProductImageResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ProductImages.ProductImageUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.ProductImages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductImageMapper implements EntityMapper<ProductImages, ProductImageResponseDTO> {

    @Override
    public ProductImages toEntity(ProductImageResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductImages.builder()
                .imageId(dto.getImageId())
                .imageUrl(dto.getImageUrl())
                .isPrimary(dto.getIsPrimary())
                .displayOrder(dto.getDisplayOrder())
                .build();
    }

    @Override
    public ProductImageResponseDTO toDto(ProductImages entity) {
        if (entity == null) {
            return null;
        }
        return ProductImageResponseDTO.builder()
                .imageId(entity.getImageId())
                .imageUrl(entity.getImageUrl())
                .isPrimary(entity.getIsPrimary())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

    @Override
    public List<ProductImages> toEntity(List<ProductImageResponseDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<ProductImageResponseDTO> toDto(List<ProductImages> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProductImages toEntity(ProductImageCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductImages.builder()
                .imageUrl(dto.getImageUrl())
                .isPrimary(dto.getIsPrimary())
                .displayOrder(dto.getDisplayOrder())
                .build();
    }

    public ProductImages toEntity(ProductImageUpdateDTO dto, ProductImages existingEntity) {
        if (dto == null || existingEntity == null) {
            return existingEntity;
        }
        existingEntity.setImageUrl(dto.getImageUrl());
        existingEntity.setIsPrimary(dto.getIsPrimary());
        existingEntity.setDisplayOrder(dto.getDisplayOrder());
        return existingEntity;
    }
}
