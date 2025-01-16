package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Categories;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryMapper implements EntityMapper<Categories, CategoryResponseDTO> {

    // Map từ CategoryResponseDTO sang Categories entity
    @Override
    public Categories toEntity(CategoryResponseDTO dto) {
        if (dto == null) {
            return null;
        }
        return Categories.builder()
                .id(dto.getCategoryId())
                .name(dto.getName())
                .parentId(dto.getParentId())
                .description(dto.getDescription())
                .slug(dto.getSlug())
                .status(dto.getStatus())
                .build();
    }

    // Map từ Categories entity sang CategoryResponseDTO
    @Override
    public CategoryResponseDTO toDto(Categories entity) {
        if (entity == null) {
            return null;
        }
        return CategoryResponseDTO.builder()
                .categoryId(entity.getId())
                .name(entity.getName())
                .parentId(entity.getParentId())
                .description(entity.getDescription())
                .slug(entity.getSlug())
                .status(entity.getStatus())
                .build();
    }

    // Map danh sách CategoryResponseDTO sang danh sách Categories entity
    @Override
    public List<Categories> toEntity(List<CategoryResponseDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return List.of();
        }
        return dtoList.stream().map(this::toEntity).collect(Collectors.toList());
    }

    // Map danh sách Categories entity sang danh sách CategoryResponseDTO
    @Override
    public List<CategoryResponseDTO> toDto(List<Categories> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return List.of();
        }
        return entityList.stream().map(this::toDto).collect(Collectors.toList());
    }

    // Map từ CategoryCreateDTO sang Categories entity
    public Categories toEntity(CategoryCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        return Categories.builder()
                .name(dto.getName())
                .parentId(dto.getParentId())
                .description(dto.getDescription())
                .slug(dto.getSlug())
                .status(dto.getStatus())
                .build();
    }

    // Map từ CategoryUpdateDTO sang Categories entity
    public Categories toEntity(CategoryUpdateDTO dto, Categories existingEntity) {
        if (dto == null || existingEntity == null) {
            return null;
        }
        existingEntity.setName(dto.getName());
        existingEntity.setParentId(dto.getParentId());
        existingEntity.setDescription(dto.getDescription());
        existingEntity.setSlug(dto.getSlug());
        existingEntity.setStatus(dto.getStatus());
        return existingEntity;
    }
}
