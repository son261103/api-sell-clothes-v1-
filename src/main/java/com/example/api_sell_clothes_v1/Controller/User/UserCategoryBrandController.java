package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandHierarchyDTO;
import com.example.api_sell_clothes_v1.DTO.Brands.BrandResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryHierarchyDTO;
import com.example.api_sell_clothes_v1.DTO.Categories.CategoryResponseDTO;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Service.BrandService;
import com.example.api_sell_clothes_v1.Service.ParentCategoryService;
import com.example.api_sell_clothes_v1.Service.SubCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/categories-and-brands")
@RequiredArgsConstructor
@Slf4j
public class UserCategoryBrandController {

    private final BrandService brandService;
    private final ParentCategoryService parentCategoryService;
    private final SubCategoryService subCategoryService;

    /**
     * Get all active brands
     */
    @GetMapping("/brands")
    public ResponseEntity<List<BrandResponseDTO>> getActiveBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    /**
     * Get brand details by ID
     */
    @GetMapping("/brands/{brandId}")
    public ResponseEntity<BrandResponseDTO> getBrandById(@PathVariable Long brandId) {
        return ResponseEntity.ok(brandService.getBrandById(brandId));
    }

    /**
     * Search brands by keyword
     */
    @GetMapping("/brands/search")
    public ResponseEntity<List<BrandResponseDTO>> searchBrands(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(brandService.searchBrands(keyword));
    }

    /**
     * Get all active parent categories
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getAllActiveParentCategories() {
        return ResponseEntity.ok(parentCategoryService.getAllActiveParentCategories());
    }

    /**
     * Get parent category by ID with its subcategories
     * Modified to handle both parent categories and subcategories
     */
    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<Map<String, Object>> getCategoryWithSubcategories(
            @PathVariable Long categoryId) {
        log.info("Fetching category with ID: {}", categoryId);

        Map<String, Object> response = new HashMap<>();

        try {
            // First, try to get it as a parent category
            try {
                CategoryHierarchyDTO hierarchy = parentCategoryService.getParentCategoryWithSubs(categoryId);

                // Transform to format expected by frontend
                response.put("category", hierarchy.getParent());
                response.put("subcategories", hierarchy.getSubCategories());

                log.info("Successfully fetched parent category with ID: {}", categoryId);
                return ResponseEntity.ok(response);
            }
            catch (IllegalArgumentException e) {
                // If it's not a parent category, it might be a subcategory
                if (e.getMessage().contains("not a parent category")) {
                    log.info("Category {} is not a parent category, trying as subcategory", categoryId);

                    CategoryResponseDTO subCategory = subCategoryService.getSubCategoryById(categoryId);

                    // Return subcategory with empty subcategories list
                    response.put("category", subCategory);
                    response.put("subcategories", new ArrayList<CategoryResponseDTO>());

                    log.info("Successfully fetched subcategory with ID: {}", categoryId);
                    return ResponseEntity.ok(response);
                } else {
                    // If it's some other IllegalArgumentException, rethrow it
                    throw e;
                }
            }
        } catch (ResourceNotFoundException e) {
            log.error("Category with ID {} not found", categoryId, e);
            throw new ResourceNotFoundException("Category with ID " + categoryId + " not found");
        } catch (Exception e) {
            log.error("Error fetching category with ID {}: {}", categoryId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get category by slug
     */
    @GetMapping("/categories/slug/{slug}")
    public ResponseEntity<CategoryResponseDTO> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(parentCategoryService.getCategoryBySlug(slug));
    }

    /**
     * Get all active subcategories for a parent category
     */
    @GetMapping("/categories/{parentId}/subcategories")
    public ResponseEntity<List<CategoryResponseDTO>> getActiveSubCategories(
            @PathVariable Long parentId) {
        return ResponseEntity.ok(subCategoryService.getAllActiveSubCategories(parentId));
    }

    /**
     * Get all brands with hierarchy information (statistics)
     */
    @GetMapping("/brands/hierarchy")
    public ResponseEntity<BrandHierarchyDTO> getBrandHierarchy() {
        return ResponseEntity.ok(brandService.getAllBrandsHierarchy());
    }

    /**
     * Get subcategory by ID
     */
    @GetMapping("/subcategories/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> getSubCategoryById(@PathVariable Long categoryId) {
        return ResponseEntity.ok(subCategoryService.getSubCategoryById(categoryId));
    }
}