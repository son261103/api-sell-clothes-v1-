package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductImages.*;
import com.example.api_sell_clothes_v1.Service.ProductImageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCTS + "/images")
@RequiredArgsConstructor
public class ProductImageController {
    private final ProductImageService imageService;
    private final ObjectMapper objectMapper;

    @GetMapping("/hierarchy/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductImageHierarchyDTO> getProductImageHierarchy(
            @PathVariable Long productId) {
        return ResponseEntity.ok(imageService.getProductImagesHierarchy(productId));
    }

    @PostMapping(value = "/upload/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> uploadSingleImage(
            @RequestParam("productId") Long productId,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder) {
        try {
            ProductImageCreateDTO createDTO = ProductImageCreateDTO.builder()
                    .productId(productId)
                    .isPrimary(isPrimary)
                    .displayOrder(displayOrder)
                    .build();

            ProductImageResponseDTO response = imageService.addProductImage(createDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error uploading image: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<List<ProductImageResponseDTO>> uploadMultipleImages(
            @RequestParam("productId") Long productId,
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam(value = "details", required = false) String imageDetailsJson) {
        try {
            List<BulkProductImageCreateDTO.ImageDetail> imageDetails = new ArrayList<>();

            // If details are provided, parse them
            if (imageDetailsJson != null && !imageDetailsJson.isEmpty()) {
                imageDetails = objectMapper.readValue(imageDetailsJson,
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, BulkProductImageCreateDTO.ImageDetail.class));
            }

            // Create bulk upload DTO
            BulkProductImageCreateDTO bulkDTO = BulkProductImageCreateDTO.builder()
                    .productId(productId)
                    .images(imageDetails)
                    .build();

            List<ProductImageResponseDTO> response = imageService.addProductImages(bulkDTO);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error uploading images: " + e.getMessage());
        }
    }

    @PutMapping("/update/{imageId}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> updateProductImage(
            @PathVariable Long imageId,
            @RequestBody ProductImageUpdateDTO updateDTO) {
        return ResponseEntity.ok(imageService.updateProductImage(imageId, updateDTO));
    }

    @DeleteMapping("/delete/{imageId}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ApiResponse> deleteProductImage(
            @PathVariable Long imageId) {
        return ResponseEntity.ok(imageService.deleteProductImage(imageId));
    }

    @PutMapping("/reorder/{productId}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ApiResponse> reorderProductImages(
            @PathVariable Long productId,
            @RequestBody List<Long> imageIds) {
        return ResponseEntity.ok(imageService.reorderProductImages(productId, imageIds));
    }

    @GetMapping("/list/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductImageResponseDTO>> getProductImages(
            @PathVariable Long productId) {
        return ResponseEntity.ok(imageService.getProductImages(productId));
    }

    @GetMapping("/primary/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductImageResponseDTO> getPrimaryImage(
            @PathVariable Long productId) {
        return ResponseEntity.ok(imageService.getPrimaryImage(productId));
    }
}