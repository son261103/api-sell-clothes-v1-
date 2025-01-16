package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Categories;
import com.example.api_sell_clothes_v1.Mapper.CategoryMapper;
import com.example.api_sell_clothes_v1.Repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    // Kiểm tra category cha có hợp lệ không
    private void validateParentCategory(Long parentId) {
        if (parentId != null) {
            Optional<Categories> parentCategoryOptional = categoryRepository.findById(parentId);
            if (parentCategoryOptional.isEmpty()) {
                throw new RuntimeException("Parent category not found with id: " + parentId);
            }
        }
    }

    // Tạo mới category
    @Transactional
    public CategoryResponseDTO createCategory(CategoryCreateDTO categoryCreateDTO) {
        // Kiểm tra tính hợp lệ của parentId
        if (categoryCreateDTO.getParentId() != null && categoryCreateDTO.getParentId().equals(categoryCreateDTO.getCategoryId())) {
            throw new RuntimeException("Category cannot be its own parent.");
        }

        // Kiểm tra tính hợp lệ của category cha
        validateParentCategory(categoryCreateDTO.getParentId());

        Categories category = categoryMapper.toEntity(categoryCreateDTO);
        Categories savedCategory = categoryRepository.save(category);
        return categoryMapper.toDto(savedCategory);
    }

    // Cập nhật category
    @Transactional
    public CategoryResponseDTO updateCategory(Long categoryId, CategoryUpdateDTO categoryUpdateDTO) {
        Optional<Categories> existingCategoryOptional = categoryRepository.findById(categoryId);
        if (existingCategoryOptional.isEmpty()) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }

        Categories existingCategory = existingCategoryOptional.get();

        // Kiểm tra tính hợp lệ của parentId
        if (categoryUpdateDTO.getParentId() != null && categoryUpdateDTO.getParentId().equals(categoryId)) {
            throw new RuntimeException("Category cannot be its own parent.");
        }

        // Kiểm tra category cha có tồn tại không
        validateParentCategory(categoryUpdateDTO.getParentId());

        categoryMapper.toEntity(categoryUpdateDTO, existingCategory);
        Categories updatedCategory = categoryRepository.save(existingCategory);

        return categoryMapper.toDto(updatedCategory);
    }

    // Lấy tất cả category
    public List<CategoryResponseDTO> getAllCategories() {
        List<Categories> categories = categoryRepository.findAll();
        return categoryMapper.toDto(categories);
    }

    // Lấy category theo ID
    public CategoryResponseDTO getCategoryById(Long categoryId) {
        Optional<Categories> categoryOptional = categoryRepository.findById(categoryId);
        if (categoryOptional.isEmpty()) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }
        return categoryMapper.toDto(categoryOptional.get());
    }

    // Xóa category
    @Transactional
    public void deleteCategory(Long categoryId) {
        Optional<Categories> categoryOptional = categoryRepository.findById(categoryId);
        if (categoryOptional.isEmpty()) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }
}
