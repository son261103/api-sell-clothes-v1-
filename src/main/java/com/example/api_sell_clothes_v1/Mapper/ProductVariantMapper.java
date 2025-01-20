package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.ProductVariant.ProductVariantCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.ProductVariantResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.ProductVariantUpdateDTO;

import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductVariantMapper implements EntityMapper<ProductVariant, ProductVariantResponseDTO> {

    private final ProductMapper productMapper;

    @Override
    public ProductVariant toEntity(ProductVariantResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductVariant.builder()
                .variantId(dto.getVariantId())
                .product(productMapper.toEntity(dto.getProduct()))
                .size(dto.getSize())
                .color(dto.getColor())
                .sku(dto.getSku())
                .stockQuantity(dto.getStockQuantity())
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus())
                .build();
    }

    @Override
    public ProductVariantResponseDTO toDto(ProductVariant entity) {
        if (entity == null) {
            return null;
        }
        return ProductVariantResponseDTO.builder()
                .variantId(entity.getVariantId())
                .product(productMapper.toDto(entity.getProduct()))
                .size(entity.getSize())
                .color(entity.getColor())
                .sku(entity.getSku())
                .stockQuantity(entity.getStockQuantity())
                .imageUrl(entity.getImageUrl())
                .status(entity.getStatus())
                .build();
    }

    @Override
    public List<ProductVariant> toEntity(List<ProductVariantResponseDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<ProductVariantResponseDTO> toDto(List<ProductVariant> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProductVariant toEntity(ProductVariantCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductVariant.builder()
                .size(dto.getSize())
                .color(dto.getColor())
                .sku(dto.getSku())
                .stockQuantity(dto.getStockQuantity())
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus())
                .build();
    }

    public ProductVariant toEntity(ProductVariantUpdateDTO dto, ProductVariant existingEntity) {
        if (dto == null || existingEntity == null) {
            return existingEntity;
        }

        if (dto.getSize() != null) {
            existingEntity.setSize(dto.getSize());
        }
        if (dto.getColor() != null) {
            existingEntity.setColor(dto.getColor());
        }
        if (dto.getSku() != null) {
            existingEntity.setSku(dto.getSku());
        }
        if (dto.getStockQuantity() != null) {
            existingEntity.setStockQuantity(dto.getStockQuantity());
        }
        if (dto.getImageUrl() != null) {
            existingEntity.setImageUrl(dto.getImageUrl());
        }
        if (dto.getStatus() != null) {
            existingEntity.setStatus(dto.getStatus());
        }

        return existingEntity;
    }
}