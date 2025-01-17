package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Categories.*;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Exceptions.SharedException;
import com.example.api_sell_clothes_v1.Mapper.CategoryMapper;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParentCategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // Create parent category (category without parent)
    public CategoryResponseDTO createParentCategory(CategoryCreateDTO createDTO) {
        // Ensure it's a parent category
        if (createDTO.getParentId() != null) {
            throw new IllegalArgumentException("Parent category cannot have a parent ID");
        }

        // Validate uniqueness of name and slug
        validateCategoryName(createDTO.getName());
        validateCategorySlug(createDTO.getSlug());

        // Set default status to active if not provided
        if (createDTO.getStatus() == null) {
            createDTO.setStatus(true);
        }

        Categories category = categoryMapper.toEntity(createDTO);
        category = categoryRepository.save(category);
        return categoryMapper.toDto(category);
    }

    // Update parent category
    public CategoryResponseDTO updateParentCategory(Long categoryId, CategoryUpdateDTO updateDTO) {
        Categories existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        // Verify it's actually a parent category
        if (existingCategory.getParentId() != null) {
            throw new IllegalArgumentException("Specified category is not a parent category");
        }

        // Ensure update doesn't try to set a parent
        if (updateDTO.getParentId() != null) {
            throw new IllegalArgumentException("Cannot set parent ID for parent category");
        }

        if (updateDTO.getName() != null && !updateDTO.getName().equals(existingCategory.getName())) {
            validateCategoryName(updateDTO.getName());
            validateCategoryName(updateDTO.getName());
            existingCategory.setName(updateDTO.getName());
        }

        if (updateDTO.getSlug() != null && !updateDTO.getSlug().equals(existingCategory.getSlug())) {
            validateCategorySlug(updateDTO.getSlug());
            existingCategory.setSlug(updateDTO.getSlug());
        }

        if (updateDTO.getDescription() != null) {
            existingCategory.setDescription(updateDTO.getDescription());
        }

        if (updateDTO.getStatus() != null) {
            existingCategory.setStatus(updateDTO.getStatus());
        }

        // Save the updated category
        Categories updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toDto(updatedCategory);
    }

    // Delete parent category and all its sub-categories
    @Transactional
    public void deleteParentCategory(Long categoryId) {
        Categories parentCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        // Verify it's actually a parent category
        if (parentCategory.getParentId() != null) {
            throw new IllegalArgumentException("Specified category is not a parent category");
        }

        // Find all sub-categories
        List<Categories> subCategories = categoryRepository.findAllByParentId(categoryId);

        // Delete sub-categories first
        if (!subCategories.isEmpty()) {
            categoryRepository.deleteAll(subCategories);
        }

        // Delete parent category
        categoryRepository.delete(parentCategory);
    }

    // Get all parent categories
    public List<CategoryResponseDTO> getAllParentCategories() {
        List<Categories> parentCategories = categoryRepository.findAllByParentIdIsNull();
        return categoryMapper.toDto(parentCategories);
    }

    // Get all active parent categories
    public List<CategoryResponseDTO> getAllActiveParentCategories() {
        List<Categories> activeParentCategories = categoryRepository.findAllByParentIdIsNullAndStatusIsTrue();
        return categoryMapper.toDto(activeParentCategories);
    }

    // Get parent category by ID
    public CategoryResponseDTO getParentCategoryById(Long categoryId) {
        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        if (category.getParentId() != null) {
            throw new IllegalArgumentException("Specified category is not a parent category");
        }

        return categoryMapper.toDto(category);
    }

    // Get parent category with all its sub-categories
    public CategoryHierarchyDTO getParentCategoryWithSubs(Long categoryId) {
        Categories parentCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        if (parentCategory.getParentId() != null) {
            throw new IllegalArgumentException("Specified category is not a parent category");
        }

        List<Categories> subCategories = categoryRepository.findAllByParentId(categoryId);

        // Tính toán các thống kê
        int totalSubCategories = subCategories.size();
        int activeSubCategories = (int) subCategories.stream()
                .filter(Categories::getStatus)
                .count();
        int inactiveSubCategories = totalSubCategories - activeSubCategories;

        return CategoryHierarchyDTO.builder()
                .parent(categoryMapper.toDto(parentCategory))
                .subCategories(categoryMapper.toDto(subCategories))
                .totalSubCategories(totalSubCategories)
                .activeSubCategories(activeSubCategories)
                .inactiveSubCategories(inactiveSubCategories)
                .build();
    }

    // Toggle category status
    public CategoryResponseDTO toggleCategoryStatus(Long categoryId) {
        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        category.setStatus(!category.getStatus()); // Toggle the status
        category = categoryRepository.save(category);
        return categoryMapper.toDto(category);
    }

    // Validation methods
    private void validateCategoryName(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new SharedException("Category with name '" + name + "' already exists");
        }
    }

    private void validateCategorySlug(String slug) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new SharedException("Category with slug '" + slug + "' already exists");
        }
    }

    private Categories findByNameOrThrow(String name) {
        return categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category with name '" + name + "' not found"));
    }

    private Categories findBySlugOrThrow(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category with slug '" + slug + "' not found"));
    }

    // Add methods to find by name or slug
    public CategoryResponseDTO getCategoryByName(String name) {
        Categories category = findByNameOrThrow(name);
        return categoryMapper.toDto(category);
    }

    public CategoryResponseDTO getCategoryBySlug(String slug) {
        Categories category = findBySlugOrThrow(slug);
        return categoryMapper.toDto(category);
    }
}