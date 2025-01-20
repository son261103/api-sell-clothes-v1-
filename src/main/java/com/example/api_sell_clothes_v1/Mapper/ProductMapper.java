package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Products.ProductCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Products;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMapper implements EntityMapper<Products, ProductResponseDTO> {

    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;

    @Override
    public Products toEntity(ProductResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        return Products.builder()
                .productId(dto.getProductId())
                .category(categoryMapper.toEntity(dto.getCategory()))
                .brand(brandMapper.toEntity(dto.getBrand()))
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .salePrice(dto.getSalePrice())
                .thumbnail(dto.getThumbnail())
                .slug(dto.getSlug())
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    @Override
    public ProductResponseDTO toDto(Products entity) {
        if (entity == null) {
            return null;
        }
        return ProductResponseDTO.builder()
                .productId(entity.getProductId())
                .category(categoryMapper.toDto(entity.getCategory()))
                .brand(brandMapper.toDto(entity.getBrand()))
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .salePrice(entity.getSalePrice())
                .thumbnail(entity.getThumbnail())
                .slug(entity.getSlug())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<Products> toEntity(List<ProductResponseDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<ProductResponseDTO> toDto(List<Products> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Products toEntity(ProductCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        return Products.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .salePrice(dto.getSalePrice())
                .thumbnail(dto.getThumbnail())
                .slug(dto.getSlug())
                .status(dto.getStatus())
                .build();
    }

    public Products toEntity(ProductUpdateDTO dto, Products existingEntity) {
        if (dto == null || existingEntity == null) {
            return existingEntity;
        }
        existingEntity.setName(dto.getName());
        existingEntity.setDescription(dto.getDescription());
        existingEntity.setPrice(dto.getPrice());
        existingEntity.setSalePrice(dto.getSalePrice());
        existingEntity.setThumbnail(dto.getThumbnail());
        existingEntity.setSlug(dto.getSlug());
        existingEntity.setStatus(dto.getStatus());
        return existingEntity;
    }
}