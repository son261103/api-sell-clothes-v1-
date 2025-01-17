package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Brands.BrandCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.Entity.Brands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BrandMapper implements EntityMapper<Brands, BrandResponseDTO> {
    @Override
    public Brands toEntity(BrandResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        return Brands.builder()
                .brandId(dto.getBrandId())
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .status(dto.getStatus())
                .build();
    }

    @Override
    public BrandResponseDTO toDto(Brands entity) {
        if (entity == null) {
            return null;
        }
        return BrandResponseDTO.builder()
                .brandId(entity.getBrandId())
                .name(entity.getName())
                .description(entity.getDescription())
                .logoUrl(entity.getLogoUrl())
                .status(entity.getStatus())
                .build();
    }

    @Override
    public List<Brands> toEntity(List<BrandResponseDTO> Dto) {
        if (Dto == null || Dto.isEmpty()) {
            return List.of();
        }
        return Dto.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public List<BrandResponseDTO> toDto(List<Brands> entity) {
        if (entity == null || entity.isEmpty()) {
            return List.of();
        }
        return entity.stream().map(this::toDto).collect(Collectors.toList());
    }

    public Brands toEntity(BrandCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        return Brands.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .logoUrl(dto.getLogoUrl())
                .status(dto.getStatus())
                .build();
    }

    public Brands toEntity(BrandUpdateDTO dto, Brands existingEntity) {
        if (dto == null || existingEntity == null) {
            return existingEntity;
        }
        existingEntity.setName(dto.getName());
        existingEntity.setDescription(dto.getDescription());
        existingEntity.setStatus(dto.getStatus());
        return existingEntity;
    }
}
