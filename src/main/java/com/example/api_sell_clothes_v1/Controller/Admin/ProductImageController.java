package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductImages.*;
import com.example.api_sell_clothes_v1.Service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCTS + "/images")
@RequiredArgsConstructor
public class ProductImageController {
    private final ProductImageService imageService;

    /**
     * Upload multiple images for a product
     */
    @PostMapping(value = "/upload/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<List<ProductImageResponseDTO>> uploadImages(
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<ProductImageResponseDTO> response = imageService.uploadProductImages(productId, files);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error uploading images: " + e.getMessage());
        }
    }

    /**
     * Update image file (replace image)
     */
    @PutMapping(value = "/update-file/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> updateImageFile(
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile newFile) {
        try {
            ProductImageResponseDTO response = imageService.updateImageFile(imageId, newFile);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error updating image: " + e.getMessage());
        }
    }

    /**
     * Update image properties
     */
    @PutMapping("/update/{imageId}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> updateImageProperties(
            @PathVariable Long imageId,
            @RequestBody ProductImageUpdateDTO updateDTO) {
        return ResponseEntity.ok(imageService.updateImageProperties(imageId, updateDTO));
    }

    /**
     * Update primary status
     */
    @PutMapping("/primary/{imageId}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> updatePrimaryImage(
            @PathVariable Long imageId) {
        return ResponseEntity.ok(imageService.updatePrimaryStatus(imageId));
    }

    /**
     * Reorder images
     */
    @PutMapping("/reorder/{productId}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ApiResponse> reorderProductImages(
            @PathVariable Long productId,
            @RequestBody List<Long> imageIds) {
        return ResponseEntity.ok(imageService.reorderProductImages(productId, imageIds));
    }

    /**
     * Delete image
     */
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
    public ResponseEntity<ApiResponse> deleteProductImage(
            @PathVariable Long imageId) {
        return ResponseEntity.ok(imageService.deleteProductImage(imageId));
    }

    /**
     * Get all product images
     */
    @GetMapping("/list/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductImageResponseDTO>> getProductImages(
            @PathVariable Long productId) {
        return ResponseEntity.ok(imageService.getProductImages(productId));
    }

    /**
     * Get primary image
     */
    @GetMapping("/primary/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> getPrimaryImage(
            @PathVariable Long productId) {
        return ResponseEntity.ok(imageService.getPrimaryImage(productId));
    }

    /**
     * Get images hierarchy
     */
    @GetMapping("/hierarchy/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductImageHierarchyDTO> getProductImageHierarchy(
            @PathVariable Long productId) {
        return ResponseEntity.ok(imageService.getProductImagesHierarchy(productId));
    }
}