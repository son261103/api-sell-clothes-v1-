package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Products.*;
import com.example.api_sell_clothes_v1.Service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(ApiPatternConstants.API_PRODUCTS)
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @PageableDefault(page = 0, size = 10, sort = "productId") Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        return ResponseEntity.ok(productService.getAllProducts(
                pageable,
                search,
                categoryId,
                brandId,
                status,
                minPrice,
                maxPrice
        ));
    }

    @GetMapping("/hierarchy")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductHierarchyDTO> getProductHierarchy() {
        return ResponseEntity.ok(productService.getProductsHierarchy());
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CREATE_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @RequestParam("product") String productCreateDTOString,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            ProductCreateDTO createDTO = objectMapper.readValue(productCreateDTOString, ProductCreateDTO.class);
            ProductResponseDTO createdProduct = productService.createProduct(createDTO, thumbnail);
            return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while processing product data: " + e.getMessage());
        }
    }

    @PutMapping(value = "/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable("id") Long productId,
            @RequestParam("product") String productUpdateDTOString,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
        try {
            ProductUpdateDTO updateDTO = objectMapper.readValue(productUpdateDTOString, ProductUpdateDTO.class);
            ProductResponseDTO updatedProduct = productService.updateProduct(productId, updateDTO, thumbnail);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing product update data: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_PRODUCT')")
    public ResponseEntity<ApiResponse> deleteProduct(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(productService.deleteProduct(productId));
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable("id") Long productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @GetMapping("/slug/{slug}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<ProductResponseDTO> getProductBySlug(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(
            @PathVariable("categoryId") Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/brand/{brandId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByBrand(
            @PathVariable("brandId") Long brandId) {
        return ResponseEntity.ok(productService.getProductsByBrand(brandId));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Page<ProductResponseDTO>> getFilteredProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean status,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getFilteredProducts(categoryId, brandId, status, pageable));
    }

    @PatchMapping("/status/{id}")
    @PreAuthorize("hasAuthority('EDIT_PRODUCT')")
    public ResponseEntity<ApiResponse> toggleProductStatus(@PathVariable("id") Long productId) {
        ApiResponse response = productService.toggleProductStatus(productId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Page<ProductResponseDTO>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean status,
            Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(
                keyword, minPrice, maxPrice, categoryId, brandId, status, pageable));
    }

    @GetMapping("/sale")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsOnSale(Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsOnSale(pageable));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<Page<ProductResponseDTO>> getLatestProducts(
            @RequestParam(defaultValue = "10") int limit,
            Pageable pageable) {
        return ResponseEntity.ok(productService.getLatestProducts(limit, pageable));
    }

    @GetMapping("/featured")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductResponseDTO>> getFeaturedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(productService.getFeaturedProducts(limit));
    }

    @GetMapping("/related/{productId}")
    @PreAuthorize("hasAuthority('VIEW_PRODUCT')")
    public ResponseEntity<List<ProductResponseDTO>> getRelatedProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(productService.getRelatedProducts(productId, limit));
    }
}