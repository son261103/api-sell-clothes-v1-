package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductImages.ProductImageResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Products.ProductResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.ProductVariantResponseDTO;
import com.example.api_sell_clothes_v1.Service.ProductImageService;
import com.example.api_sell_clothes_v1.Service.ProductService;
import com.example.api_sell_clothes_v1.Service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/products")
@RequiredArgsConstructor
@Slf4j
public class UserProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final ProductVariantService productVariantService;

    /**
     * Main product listing with various filter options
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {

        // For public API, always filter to only show active products
        Boolean status = true;

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // Get products with filters
        Page<ProductResponseDTO> products = productService.getAllProducts(
                pageable, search, categoryId, brandId, status, minPrice, maxPrice);

        return ResponseEntity.ok(products);
    }

    /**
     * Get product details by ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDTO> getProductById(
            @PathVariable Long productId) {
        ProductResponseDTO product = productService.getProductById(productId);
        return ResponseEntity.ok(product);
    }

    /**
     * Get product by slug (SEO-friendly URL)
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponseDTO> getProductBySlug(
            @PathVariable String slug) {
        ProductResponseDTO product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(product);
    }

    /**
     * Get products by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(
            @PathVariable Long categoryId) {
        List<ProductResponseDTO> products = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products by brand
     */
    @GetMapping("/brand/{brandId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByBrand(
            @PathVariable Long brandId) {
        List<ProductResponseDTO> products = productService.getProductsByBrand(brandId);
        return ResponseEntity.ok(products);
    }

    /**
     * Get featured products
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponseDTO>> getFeaturedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        List<ProductResponseDTO> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(products);
    }

    /**
     * Get latest products
     */
    @GetMapping("/latest")
    public ResponseEntity<Page<ProductResponseDTO>> getLatestProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "8") int limit) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductResponseDTO> products = productService.getLatestProducts(limit, pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products on sale
     */
    @GetMapping("/on-sale")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsOnSale(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDTO> products = productService.getProductsOnSale(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Get related products
     */
    @GetMapping("/related/{productId}")
    public ResponseEntity<List<ProductResponseDTO>> getRelatedProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {

        List<ProductResponseDTO> relatedProducts = productService.getRelatedProducts(productId, limit);
        return ResponseEntity.ok(relatedProducts);
    }

    /**
     * Search products with various filters
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponseDTO>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Only active products should be shown to users
        Boolean status = true;

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDTO> products = productService.searchProducts(
                keyword, minPrice, maxPrice, categoryId, brandId, status, pageable);

        return ResponseEntity.ok(products);
    }

    /**
     * Get filtered products
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponseDTO>> getFilteredProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Only active products should be shown to users
        Boolean status = true;

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductResponseDTO> products = productService.getFilteredProducts(categoryId, brandId, status, pageable);
        return ResponseEntity.ok(products);
    }

    /* Product Images Endpoints */

    /**
     * Get all images for a product
     */
    @GetMapping("/{productId}/images")
    public ResponseEntity<List<ProductImageResponseDTO>> getProductImages(
            @PathVariable Long productId) {
        List<ProductImageResponseDTO> images = productImageService.getProductImages(productId);
        return ResponseEntity.ok(images);
    }

    /**
     * Get primary image for a product
     */
    @GetMapping("/{productId}/images/primary")
    public ResponseEntity<ProductImageResponseDTO> getPrimaryImage(
            @PathVariable Long productId) {
        ProductImageResponseDTO image = productImageService.getPrimaryImage(productId);
        return ResponseEntity.ok(image);
    }

    /* Product Variants Endpoints */

    /**
     * Get all active variants for a product
     */
    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantResponseDTO>> getActiveVariants(
            @PathVariable Long productId) {
        List<ProductVariantResponseDTO> variants = productVariantService.getActiveVariantsByProductId(productId);
        return ResponseEntity.ok(variants);
    }

    /**
     * Get available sizes for a product
     */
    @GetMapping("/{productId}/variants/sizes")
    public ResponseEntity<List<String>> getAvailableSizes(
            @PathVariable Long productId) {
        List<String> sizes = productVariantService.getAvailableSizes(productId);
        return ResponseEntity.ok(sizes);
    }

    /**
     * Get available colors for a product
     */
    @GetMapping("/{productId}/variants/colors")
    public ResponseEntity<List<String>> getAvailableColors(
            @PathVariable Long productId) {
        List<String> colors = productVariantService.getAvailableColors(productId);
        return ResponseEntity.ok(colors);
    }

    /**
     * Get variant details by ID
     */
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<ProductVariantResponseDTO> getVariantById(
            @PathVariable Long variantId) {
        ProductVariantResponseDTO variant = productVariantService.getVariantById(variantId);
        return ResponseEntity.ok(variant);
    }

    /**
     * Get variant by SKU
     */
    @GetMapping("/variants/sku/{sku}")
    public ResponseEntity<ProductVariantResponseDTO> getVariantBySku(
            @PathVariable String sku) {
        ProductVariantResponseDTO variant = productVariantService.getVariantBySku(sku);
        return ResponseEntity.ok(variant);
    }

    /**
     * Check if a specific variant (size/color combination) is available
     */
    @GetMapping("/{productId}/check-availability")
    public ResponseEntity<Map<String, Boolean>> checkVariantAvailability(
            @PathVariable Long productId,
            @RequestParam String size,
            @RequestParam String color) {

        boolean isAvailable = productVariantService.isVariantAvailable(productId, size, color);
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }
}