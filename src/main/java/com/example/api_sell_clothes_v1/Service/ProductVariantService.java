package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductVariant.*;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.ProductVariantMapper;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import com.example.api_sell_clothes_v1.Repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantService {
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper variantMapper;
    private final ProductVariantImageService imageService;

    private static final int LOW_STOCK_THRESHOLD = 5;

    /**
     * Get variant hierarchy information including statistics
     */
    @Transactional(readOnly = true)
    public ProductVariantHierarchyDTO getVariantHierarchy(Long productId) {
        List<ProductVariant> variants = variantRepository.findByProductProductId(productId);

        Map<String, Integer> stockBySize = new HashMap<>();
        Map<String, Integer> stockByColor = new HashMap<>();
        int totalStock = 0;

        for (ProductVariant variant : variants) {
            // Accumulate stock by size
            stockBySize.merge(variant.getSize(), variant.getStockQuantity(), Integer::sum);

            // Accumulate stock by color
            stockByColor.merge(variant.getColor(), variant.getStockQuantity(), Integer::sum);

            // Calculate total stock
            totalStock += variant.getStockQuantity();
        }

        return ProductVariantHierarchyDTO.builder()
                .variants(variantMapper.toDto(variants))
                .productId(productId)
                .totalVariants(variants.size())
                .activeVariants((int) variants.stream().filter(ProductVariant::getStatus).count())
                .inactiveVariants((int) variants.stream().filter(v -> !v.getStatus()).count())
                .totalStock(totalStock)
                .stockBySize(stockBySize)
                .stockByColor(stockByColor)
                .build();
    }

    /**
     * Create new variant
     */
    @Transactional
    public ProductVariantResponseDTO createVariant(ProductVariantCreateDTO createDTO, MultipartFile imageFile) {
        // Validate product
        Products product = productRepository.findById(createDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getStatus()) {
            throw new IllegalArgumentException("Cannot create variant for inactive product");
        }

        // Validate SKU uniqueness
        if (createDTO.getSku() != null && variantRepository.existsBySku(createDTO.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }

        // Handle image upload
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = imageService.uploadImage(imageFile);
            createDTO.setImageUrl(imageUrl);
        }

        // Set default status if not provided
        if (createDTO.getStatus() == null) {
            createDTO.setStatus(true);
        }

        // Create variant entity
        ProductVariant variant = variantMapper.toEntity(createDTO);
        variant.setProduct(product);
        variant.setCreatedAt(LocalDateTime.now());
        variant.setUpdatedAt(LocalDateTime.now());

        try {
            ProductVariant savedVariant = variantRepository.save(variant);
            log.info("Created new product variant with ID: {}", savedVariant.getVariantId());
            return variantMapper.toDto(savedVariant);
        } catch (Exception e) {
            // If variant creation fails, delete the uploaded image
            if (createDTO.getImageUrl() != null) {
                imageService.deleteImage(createDTO.getImageUrl());
            }
            throw e;
        }
    }

    /**
     * Update variant
     */
    @Transactional
    public ProductVariantResponseDTO updateVariant(Long variantId, ProductVariantUpdateDTO updateDTO, MultipartFile imageFile) {
        ProductVariant existingVariant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

        // Validate SKU uniqueness if changing
        if (updateDTO.getSku() != null && !updateDTO.getSku().equals(existingVariant.getSku())
                && variantRepository.existsBySku(updateDTO.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }

        // Handle image update
        if (imageFile != null && !imageFile.isEmpty()) {
            String newImageUrl = imageService.updateImage(imageFile, existingVariant.getImageUrl());
            updateDTO.setImageUrl(newImageUrl);
        }

        // Update variant using mapper
        ProductVariant updatedVariant = variantMapper.toEntity(updateDTO, existingVariant);
        updatedVariant.setUpdatedAt(LocalDateTime.now());

        ProductVariant savedVariant = variantRepository.save(updatedVariant);
        log.info("Updated product variant with ID: {}", savedVariant.getVariantId());

        return variantMapper.toDto(savedVariant);
    }

    /**
     * Delete variant
     */
    @Transactional
    public ApiResponse deleteVariant(Long variantId) {
        try {
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            // Delete image if exists
            if (variant.getImageUrl() != null && !variant.getImageUrl().isEmpty()) {
                imageService.deleteImage(variant.getImageUrl());
            }

            variantRepository.delete(variant);
            log.info("Deleted product variant with ID: {}", variantId);

            return new ApiResponse(true, "Product variant successfully deleted");
        } catch (DataIntegrityViolationException e) {
            log.error("Failed to delete variant due to data integrity violation: {}", e.getMessage());
            throw new IllegalStateException("Cannot delete variant as it is referenced by other entities");
        }
    }

    /**
     * Get variant by ID
     */
    @Transactional(readOnly = true)
    public ProductVariantResponseDTO getVariantById(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        return variantMapper.toDto(variant);
    }


    @Transactional
    public List<ProductVariantResponseDTO> createBulkVariants(BulkProductVariantCreateDTO bulkDTO,
                                                              Map<String, MultipartFile> colorImages) {
        // Validate product
        Products product = productRepository.findById(bulkDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getStatus()) {
            throw new IllegalArgumentException("Cannot create variants for inactive product");
        }

        // Map to store image URLs by color
        Map<String, String> colorImageUrls = new HashMap<>();

        // Upload images for each color
        if (colorImages != null && !colorImages.isEmpty()) {
            for (Map.Entry<String, MultipartFile> entry : colorImages.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String imageUrl = imageService.uploadImage(entry.getValue());
                    colorImageUrls.put(entry.getKey(), imageUrl);
                }
            }
        }

        List<ProductVariant> createdVariants = new ArrayList<>();

        for (BulkProductVariantCreateDTO.VariantDetail variantDetail : bulkDTO.getVariants()) {
            // Generate SKU if not provided
            if (variantDetail.getSku() == null || variantDetail.getSku().isEmpty()) {
                String generatedSku = generateSku(product.getName(), variantDetail.getColor(), variantDetail.getSize());
                variantDetail.setSku(generatedSku);
            }

            // Validate SKU uniqueness
            if (variantRepository.existsBySku(variantDetail.getSku())) {
                throw new IllegalArgumentException("SKU already exists: " + variantDetail.getSku());
            }

            // Get image URL for this variant's color
            String imageUrl = colorImageUrls.get(variantDetail.getColor());

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .size(variantDetail.getSize())
                    .color(variantDetail.getColor())
                    .sku(variantDetail.getSku())
                    .stockQuantity(variantDetail.getStockQuantity())
                    .imageUrl(imageUrl)
                    .status(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            createdVariants.add(variant);
        }

        List<ProductVariant> savedVariants = variantRepository.saveAll(createdVariants);
        log.info("Created {} variants for product ID: {}", savedVariants.size(), product.getProductId());

        return variantMapper.toDto(savedVariants);
    }

    private String generateSku(String productName, String color, String size) {
        String baseCode = productName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String colorCode = color.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String sizeCode = size.toUpperCase();

        return String.format("%s-%s-%s", baseCode, colorCode, sizeCode);
    }

    /**
     * Get variant by SKU
     */
    @Transactional(readOnly = true)
    public ProductVariantResponseDTO getVariantBySku(String sku) {
        ProductVariant variant = variantRepository.findBySku(sku)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        return variantMapper.toDto(variant);
    }

    /**
     * Get variants by product ID
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponseDTO> getVariantsByProductId(Long productId) {
        List<ProductVariant> variants = variantRepository.findByProductProductId(productId);
        return variantMapper.toDto(variants);
    }

    /**
     * Get active variants by product ID
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponseDTO> getActiveVariantsByProductId(Long productId) {
        List<ProductVariant> variants = variantRepository.findByProductProductIdAndStatusIsTrue(productId);
        return variantMapper.toDto(variants);
    }

    /**
     * Get filtered variants with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductVariantResponseDTO> getFilteredVariants(
            Long productId, String size, String color, Boolean status, Pageable pageable) {
        Page<ProductVariant> variantsPage = variantRepository.findByFilters(productId, size, color, status, pageable);
        return variantsPage.map(variantMapper::toDto);
    }

    /**
     * Get available sizes for a product
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableSizes(Long productId) {
        return variantRepository.findDistinctSizesByProductId(productId);
    }

    /**
     * Get available colors for a product
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableColors(Long productId) {
        return variantRepository.findDistinctColorsByProductId(productId);
    }

    /**
     * Check variant availability
     */
    @Transactional(readOnly = true)
    public boolean isVariantAvailable(Long productId, String size, String color) {
        return variantRepository.findAvailableVariant(productId, size, color).isPresent();
    }

    /**
     * Get low stock variants
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponseDTO> getLowStockVariants() {
        List<ProductVariant> variants = variantRepository.findLowStockVariants(LOW_STOCK_THRESHOLD);
        return variantMapper.toDto(variants);
    }

    /**
     * Get out of stock variants
     */
    @Transactional(readOnly = true)
    public List<ProductVariantResponseDTO> getOutOfStockVariants() {
        List<ProductVariant> variants = variantRepository.findOutOfStockVariants();
        return variantMapper.toDto(variants);
    }

    /**
     * Toggle variant status
     */
    @Transactional
    public ApiResponse toggleVariantStatus(Long variantId) {
        try {
            ProductVariant variant = variantRepository.findById(variantId)
                    .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

            // Check product status before activating variant
            if (!variant.getStatus() && !variant.getProduct().getStatus()) {
                throw new IllegalArgumentException("Cannot activate variant when product is inactive");
            }

            variant.setStatus(!variant.getStatus());
            variant.setUpdatedAt(LocalDateTime.now());
            variantRepository.save(variant);

            String statusMessage = variant.getStatus() ? "activated" : "deactivated";
            log.info("Variant ID {} has been {}", variantId, statusMessage);

            return new ApiResponse(true, "Variant has been " + statusMessage);
        } catch (Exception e) {
            log.error("Error toggling variant status: {}", e.getMessage());
            return new ApiResponse(false, "Error updating variant status: " + e.getMessage());
        }
    }

    /**
     * Update stock quantity
     */
    @Transactional
    public ProductVariantResponseDTO updateStockQuantity(Long variantId, Integer quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found"));

        variant.setStockQuantity(quantity);
        variant.setUpdatedAt(LocalDateTime.now());

        ProductVariant savedVariant = variantRepository.save(variant);
        log.info("Updated stock quantity for variant ID {}: {}", variantId, quantity);

        return variantMapper.toDto(savedVariant);
    }
}