package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryHierarchyDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryUpdateDTO;
import com.example.api_sell_clothes_v1.Service.ParentCategoryService;
import com.example.api_sell_clothes_v1.Service.SubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPatternConstants.API_CATEGORIES)
@RequiredArgsConstructor
public class CategoryController {
    private final ParentCategoryService parentCategoryService;
    private final SubCategoryService subCategoryService;

    // Parent Category endpoints
    @GetMapping("/parent/list")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<List<CategoryResponseDTO>> getAllParentCategories() {
        return ResponseEntity.ok(parentCategoryService.getAllParentCategories());
    }

    @GetMapping("/parent/list/active")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<List<CategoryResponseDTO>> getAllActiveParentCategories() {
        return ResponseEntity.ok(parentCategoryService.getAllActiveParentCategories());
    }

    @GetMapping("/parent/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> getParentCategoryById(@PathVariable("id") Long categoryId) {
        return ResponseEntity.ok(parentCategoryService.getParentCategoryById(categoryId));
    }

    @PostMapping("/parent/create")
    @PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> createParentCategory(@RequestBody CategoryCreateDTO createDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parentCategoryService.createParentCategory(createDTO));
    }

    @PutMapping("/parent/edit/{id}")
    @PreAuthorize("hasAuthority('EDIT_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> updateParentCategory(
            @PathVariable("id") Long categoryId,
            @RequestBody CategoryUpdateDTO updateDTO) {
        return ResponseEntity.ok(parentCategoryService.updateParentCategory(categoryId, updateDTO));
    }

    @PutMapping("/parent/toggle-status/{id}")
    @PreAuthorize("hasAuthority('EDIT_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> toggleParentCategoryStatus(@PathVariable("id") Long categoryId) {
        return ResponseEntity.ok(parentCategoryService.toggleCategoryStatus(categoryId));
    }

    @DeleteMapping("/parent/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_CATEGORY')")
    public ResponseEntity<ApiResponse> deleteParentCategory(@PathVariable("id") Long categoryId) {
        parentCategoryService.deleteParentCategory(categoryId);
        return ResponseEntity.ok(new ApiResponse(true, "Parent category deleted successfully"));
    }

    @GetMapping("/parent/{id}/hierarchy")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<CategoryHierarchyDTO> getParentCategoryWithSubs(@PathVariable("id") Long categoryId) {
        return ResponseEntity.ok(parentCategoryService.getParentCategoryWithSubs(categoryId));
    }

    @GetMapping("/parent/by-name/{name}")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> getParentCategoryByName(@PathVariable("name") String name) {
        return ResponseEntity.ok(parentCategoryService.getCategoryByName(name));
    }

    @GetMapping("/parent/by-slug/{slug}")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> getParentCategoryBySlug(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(parentCategoryService.getCategoryBySlug(slug));
    }

    // Sub-Category endpoints
    @GetMapping("/sub/list/{parentId}")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<List<CategoryResponseDTO>> getAllSubCategories(@PathVariable("parentId") Long parentId) {
        return ResponseEntity.ok(subCategoryService.getAllSubCategories(parentId));
    }

    @GetMapping("/sub/list/active/{parentId}")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<List<CategoryResponseDTO>> getAllActiveSubCategories(@PathVariable("parentId") Long parentId) {
        return ResponseEntity.ok(subCategoryService.getAllActiveSubCategories(parentId));
    }

    @GetMapping("/sub/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> getSubCategoryById(@PathVariable("id") Long categoryId) {
        return ResponseEntity.ok(subCategoryService.getSubCategoryById(categoryId));
    }

    @PostMapping("/sub/create/{parentId}")
    @PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> createSubCategory(
            @PathVariable("parentId") Long parentId,
            @RequestBody CategoryCreateDTO createDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subCategoryService.createSubCategory(parentId, createDTO));
    }

    @PutMapping("/sub/edit/{id}")
    @PreAuthorize("hasAuthority('EDIT_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> updateSubCategory(
            @PathVariable("id") Long categoryId,
            @RequestBody CategoryUpdateDTO updateDTO) {
        return ResponseEntity.ok(subCategoryService.updateSubCategory(categoryId, updateDTO));
    }

    @PutMapping("/sub/toggle-status/{id}")
    @PreAuthorize("hasAuthority('EDIT_CATEGORY')")
    public ResponseEntity<CategoryResponseDTO> toggleSubCategoryStatus(@PathVariable("id") Long categoryId) {
        return ResponseEntity.ok(subCategoryService.toggleSubCategoryStatus(categoryId));
    }

    @DeleteMapping("/sub/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_CATEGORY')")
    public ResponseEntity<ApiResponse> deleteSubCategory(@PathVariable("id") Long categoryId) {
        subCategoryService.deleteSubCategory(categoryId);
        return ResponseEntity.ok(new ApiResponse(true, "Sub-category deleted successfully"));
    }
}