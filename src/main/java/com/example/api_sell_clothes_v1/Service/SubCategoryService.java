package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Exceptions.SharedException;
import com.example.api_sell_clothes_v1.Mapper.CategoryMapper;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubCategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // Create sub-category
    public CategoryResponseDTO createSubCategory(Long parentId, CategoryCreateDTO createDTO) {
        // Verify parent exists
        Categories parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        // Verify parent is actually a parent category
        if (parentCategory.getParentId() != null) {
            throw new IllegalArgumentException("Specified parent is already a sub-category");
        }

        // Verify parent is active
        if (!parentCategory.getStatus()) {
            throw new IllegalArgumentException("Cannot create sub-category under inactive parent category");
        }

        // Validate uniqueness of name and slug
        validateCategoryName(createDTO.getName());
        validateCategorySlug(createDTO.getSlug());

        // Set parent ID
        createDTO.setParentId(parentId);

        // Set default status to active if not provided
        if (createDTO.getStatus() == null) {
            createDTO.setStatus(true);
        }

        Categories subCategory = categoryMapper.toEntity(createDTO);
        subCategory = categoryRepository.save(subCategory);
        return categoryMapper.toDto(subCategory);
    }

    public CategoryResponseDTO updateSubCategory(Long categoryId, CategoryUpdateDTO updateDTO) {
        Categories existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found"));

        // Verify it's actually a sub-category
        if (existingCategory.getParentId() == null) {
            throw new IllegalArgumentException("Specified category is not a sub-category");
        }

        // Validate name and slug if they're being updated
        String newName = updateDTO.getName() != null ? updateDTO.getName() : existingCategory.getName();
        String newSlug = updateDTO.getSlug() != null ? updateDTO.getSlug() : existingCategory.getSlug();
        String newDescription = updateDTO.getDescription() != null ? updateDTO.getDescription() : existingCategory.getDescription();
        Long newParentId = updateDTO.getParentId() != null ? updateDTO.getParentId() : existingCategory.getParentId();
        Boolean newStatus = updateDTO.getStatus() != null ? updateDTO.getStatus() : existingCategory.getStatus();

        // Validate new name if different from existing
        if (!newName.equals(existingCategory.getName())) {
            validateCategoryName(newName);
        }

        // Validate new slug if different from existing
        if (!newSlug.equals(existingCategory.getSlug())) {
            validateCategorySlug(newSlug);
        }

        // Verify new parent if parent ID is being changed
        if (!newParentId.equals(existingCategory.getParentId())) {
            Categories newParent = categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new ResourceNotFoundException("New parent category not found"));

            if (newParent.getParentId() != null) {
                throw new IllegalArgumentException("New parent is already a sub-category");
            }

            if (!newParent.getStatus()) {
                throw new IllegalArgumentException("Cannot move sub-category under inactive parent category");
            }
        }

        // Set all values, using new values where provided and existing values where not
        existingCategory.setName(newName);
        existingCategory.setSlug(newSlug);
        existingCategory.setParentId(newParentId);
        existingCategory.setDescription(newDescription);
        existingCategory.setStatus(newStatus);

        Categories updatedCategory = categoryRepository.save(existingCategory);
        return categoryMapper.toDto(updatedCategory);
    }
    // Delete sub-category
    public void deleteSubCategory(Long categoryId) {
        Categories subCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found"));

        // Verify it's actually a sub-category
        if (subCategory.getParentId() == null) {
            throw new IllegalArgumentException("Specified category is not a sub-category");
        }

        categoryRepository.delete(subCategory);
    }

    // Get all sub-categories of a parent
    public List<CategoryResponseDTO> getAllSubCategories(Long parentId) {
        // Verify parent exists and is a parent category
        Categories parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        if (parentCategory.getParentId() != null) {
            throw new IllegalArgumentException("Specified category is not a parent category");
        }

        List<Categories> subCategories = categoryRepository.findAllByParentId(parentId);
        return categoryMapper.toDto(subCategories);
    }

    // Get all active sub-categories of a parent
    public List<CategoryResponseDTO> getAllActiveSubCategories(Long parentId) {
        // Verify parent exists, is a parent category, and is active
        Categories parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

        if (parentCategory.getParentId() != null) {
            throw new IllegalArgumentException("Specified category is not a parent category");
        }

        if (!parentCategory.getStatus()) {
            throw new IllegalArgumentException("Parent category is inactive");
        }

        List<Categories> activeSubCategories = categoryRepository.findAllByParentIdAndStatusIsTrue(parentId);
        return categoryMapper.toDto(activeSubCategories);
    }

    // Get sub-category by ID
    public CategoryResponseDTO getSubCategoryById(Long categoryId) {
        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found"));

        if (category.getParentId() == null) {
            throw new IllegalArgumentException("Specified category is not a sub-category");
        }

        return categoryMapper.toDto(category);
    }

    // Toggle sub-category status
    public CategoryResponseDTO toggleSubCategoryStatus(Long categoryId) {
        Categories category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-category not found"));

        if (category.getParentId() == null) {
            throw new IllegalArgumentException("Specified category is not a sub-category");
        }

        // Verify parent category is active before activating sub-category
        if (!category.getStatus()) { // If we're activating the sub-category
            Categories parentCategory = categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found"));

            if (!parentCategory.getStatus()) {
                throw new IllegalArgumentException("Cannot activate sub-category when parent category is inactive");
            }
        }

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
}