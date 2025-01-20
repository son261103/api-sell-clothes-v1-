package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.*;
import com.example.api_sell_clothes_v1.Service.ProductVariantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCT_VARIANTS)
@RequiredArgsConstructor
public class ProductVariantController {
    private final ProductVariantService variantService;
    private final ObjectMapper objectMapper;

    @GetMapping("/hierarchy/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductVariantHierarchyDTO> getVariantHierarchy(
            @PathVariable Long productId) {
        return ResponseEntity.ok(variantService.getVariantHierarchy(productId));
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    public ResponseEntity<ProductVariantResponseDTO> createVariant(
            @RequestParam("variant") String variantCreateDTOString,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            ProductVariantCreateDTO createDTO = objectMapper.readValue(variantCreateDTOString, ProductVariantCreateDTO.class);
            ProductVariantResponseDTO createdVariant = variantService.createVariant(createDTO, image);
            return new ResponseEntity<>(createdVariant, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while processing variant data: " + e.getMessage());
        }
    }

    @PutMapping(value = "/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductVariantResponseDTO> updateVariant(
            @PathVariable("id") Long variantId,
            @RequestParam("variant") String variantUpdateDTOString,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            ProductVariantUpdateDTO updateDTO = objectMapper.readValue(variantUpdateDTOString, ProductVariantUpdateDTO.class);
            ProductVariantResponseDTO updatedVariant = variantService.updateVariant(variantId, updateDTO, image);
            return ResponseEntity.ok(updatedVariant);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing variant update data: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
    public ResponseEntity<ApiResponse> deleteVariant(@PathVariable("id") Long variantId) {
        return ResponseEntity.ok(variantService.deleteVariant(variantId));
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductVariantResponseDTO> getVariantById(@PathVariable("id") Long variantId) {
        return ResponseEntity.ok(variantService.getVariantById(variantId));
    }

    @GetMapping("/sku/{sku}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductVariantResponseDTO> getVariantBySku(@PathVariable("sku") String sku) {
        return ResponseEntity.ok(variantService.getVariantBySku(sku));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductVariantResponseDTO>> getVariantsByProductId(
            @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(variantService.getVariantsByProductId(productId));
    }

    @GetMapping("/product/{productId}/active")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductVariantResponseDTO>> getActiveVariantsByProductId(
            @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(variantService.getActiveVariantsByProductId(productId));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Page<ProductVariantResponseDTO>> getFilteredVariants(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) Boolean status,
            Pageable pageable) {
        return ResponseEntity.ok(variantService.getFilteredVariants(productId, size, color, status, pageable));
    }

    @PostMapping(value = "/bulk-create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    public ResponseEntity<List<ProductVariantResponseDTO>> createBulkVariants(
            @RequestParam("variants") String bulkCreateDTOString,
            @RequestParam Map<String, MultipartFile> images) {
        try {
            BulkProductVariantCreateDTO bulkDTO = objectMapper.readValue(
                    bulkCreateDTOString,
                    BulkProductVariantCreateDTO.class);

            // Filter out the 'variants' parameter from the images map
            Map<String, MultipartFile> colorImages = images.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals("variants"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<ProductVariantResponseDTO> createdVariants = variantService.createBulkVariants(bulkDTO, colorImages);
            return new ResponseEntity<>(createdVariants, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while processing variants data: " + e.getMessage());
        }
    }

    @GetMapping("/product/{productId}/sizes")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<String>> getAvailableSizes(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(variantService.getAvailableSizes(productId));
    }

    @GetMapping("/product/{productId}/colors")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<String>> getAvailableColors(@PathVariable("productId") Long productId) {
        return ResponseEntity.ok(variantService.getAvailableColors(productId));
    }

    @GetMapping("/check-availability")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Boolean> checkVariantAvailability(
            @RequestParam Long productId,
            @RequestParam String size,
            @RequestParam String color) {
        return ResponseEntity.ok(variantService.isVariantAvailable(productId, size, color));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductVariantResponseDTO>> getLowStockVariants() {
        return ResponseEntity.ok(variantService.getLowStockVariants());
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductVariantResponseDTO>> getOutOfStockVariants() {
        return ResponseEntity.ok(variantService.getOutOfStockVariants());
    }

    @PatchMapping("/status/{id}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ApiResponse> toggleVariantStatus(@PathVariable("id") Long variantId) {
        ApiResponse response = variantService.toggleVariantStatus(variantId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductVariantResponseDTO> updateStockQuantity(
            @PathVariable("id") Long variantId,
            @RequestParam("quantity") Integer quantity) {
        return ResponseEntity.ok(variantService.updateStockQuantity(variantId, quantity));
    }
}