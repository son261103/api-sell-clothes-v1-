package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductImages.*;
import com.example.api_sell_clothes_v1.Entity.ProductImages;
import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.ProductImageMapper;
import com.example.api_sell_clothes_v1.Repository.ProductImageRepository;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final ProductImageMapper imageMapper;
    private final CloudinaryService cloudinaryService;

    private static final String IMAGE_FOLDER = "products/images";
    private static final String IMAGE_PREFIX = "product_";

    /**
     * Get product images hierarchy information
     */
    @Transactional(readOnly = true)
    public ProductImageHierarchyDTO getProductImagesHierarchy(Long productId) {
        List<ProductImages> images = imageRepository.findByProductProductIdOrderByDisplayOrderAsc(productId);
        long primaryCount = images.stream().filter(ProductImages::getIsPrimary).count();

        return ProductImageHierarchyDTO.builder()
                .images(imageMapper.toDto(images))
                .productId(productId)
                .totalImages(images.size())
                .primaryImages((int) primaryCount)
                .nonPrimaryImages(images.size() - (int) primaryCount)
                .build();
    }

    /**
     * Add a single product image
     */
    @Transactional
    public ProductImageResponseDTO addProductImage(ProductImageCreateDTO createDTO) {
        Products product = productRepository.findById(createDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Set display order if not provided
        if (createDTO.getDisplayOrder() == null) {
            Optional<Integer> maxOrder = imageRepository.findMaxDisplayOrderByProductId(createDTO.getProductId());
            createDTO.setDisplayOrder(maxOrder.map(order -> order + 1).orElse(0));
        }

        // Set primary status for first image
        if (createDTO.getIsPrimary() == null) {
            long imageCount = imageRepository.countByProductId(createDTO.getProductId());
            createDTO.setIsPrimary(imageCount == 0);
        }

        // If this is set as primary, update other images
        if (Boolean.TRUE.equals(createDTO.getIsPrimary())) {
            handlePrimaryImageUpdate(createDTO.getProductId(), null);
        }

        ProductImages image = imageMapper.toEntity(createDTO);
        image.setProduct(product);

        ProductImages savedImage = imageRepository.save(image);
        log.info("Added new product image with ID: {}", savedImage.getImageId());

        return imageMapper.toDto(savedImage);
    }

    /**
     * Bulk add product images
     */
    @Transactional
    public List<ProductImageResponseDTO> addProductImages(BulkProductImageCreateDTO bulkDTO) {
        Products product = productRepository.findById(bulkDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Optional<Integer> maxOrder = imageRepository.findMaxDisplayOrderByProductId(bulkDTO.getProductId());
        int startOrder = maxOrder.map(order -> order + 1).orElse(0);

        long existingCount = imageRepository.countByProductId(bulkDTO.getProductId());
        boolean hasPrimary = existingCount > 0;

        List<ProductImages> newImages = new ArrayList<>();
        int orderCounter = startOrder;

        for (BulkProductImageCreateDTO.ImageDetail imageDetail : bulkDTO.getImages()) {
            ProductImages image = new ProductImages();
            image.setProduct(product);
            image.setImageUrl(imageDetail.getImageUrl());

            // Set display order if not provided
            image.setDisplayOrder(imageDetail.getDisplayOrder() != null ?
                    imageDetail.getDisplayOrder() : orderCounter++);

            // Set primary status
            boolean isPrimary = imageDetail.getIsPrimary() != null ?
                    imageDetail.getIsPrimary() : (!hasPrimary && newImages.isEmpty());
            image.setIsPrimary(isPrimary);

            if (isPrimary) {
                handlePrimaryImageUpdate(bulkDTO.getProductId(), null);
                hasPrimary = true;
            }

            newImages.add(image);
        }

        List<ProductImages> savedImages = imageRepository.saveAll(newImages);
        log.info("Added {} new product images", savedImages.size());

        return imageMapper.toDto(savedImages);
    }

    /**
     * Update product image
     */
    @Transactional
    public ProductImageResponseDTO updateProductImage(Long imageId, ProductImageUpdateDTO updateDTO) {
        ProductImages existingImage = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Product image not found"));

        // If updating to primary, handle other images
        if (Boolean.TRUE.equals(updateDTO.getIsPrimary())) {
            handlePrimaryImageUpdate(existingImage.getProduct().getProductId(), imageId);
        }

        // Update fields
        ProductImages updatedImage = imageMapper.toEntity(updateDTO, existingImage);
        ProductImages savedImage = imageRepository.save(updatedImage);

        log.info("Updated product image with ID: {}", savedImage.getImageId());
        return imageMapper.toDto(savedImage);
    }

    /**
     * Delete product image
     */
    @Transactional
    public ApiResponse deleteProductImage(Long imageId) {
        ProductImages image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Product image not found"));

        // If this was a primary image, assign primary to the first remaining image
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            List<ProductImages> remainingImages = imageRepository
                    .findByProductProductIdOrderByDisplayOrderAsc(image.getProduct().getProductId());

            // Remove current image from list
            remainingImages.removeIf(img -> img.getImageId().equals(imageId));

            if (!remainingImages.isEmpty()) {
                ProductImages newPrimary = remainingImages.get(0);
                newPrimary.setIsPrimary(true);
                imageRepository.save(newPrimary);
            }
        }

        // Delete image from storage
        try {
            cloudinaryService.deleteFile(image.getImageUrl());
        } catch (Exception e) {
            log.warn("Failed to delete image file: {}", e.getMessage());
        }

        imageRepository.delete(image);
        log.info("Deleted product image with ID: {}", imageId);

        return new ApiResponse(true, "Product image successfully deleted");
    }

    /**
     * Reorder product images
     */
    @Transactional
    public ApiResponse reorderProductImages(Long productId, List<Long> imageIds) {
        int order = 0;
        for (Long imageId : imageIds) {
            ProductImages image = imageRepository.findById(imageId)
                    .orElseThrow(() -> new EntityNotFoundException("Product image not found: " + imageId));

            if (!image.getProduct().getProductId().equals(productId)) {
                throw new IllegalArgumentException("Image does not belong to specified product");
            }

            image.setDisplayOrder(order++);
            imageRepository.save(image);
        }

        log.info("Reordered images for product ID: {}", productId);
        return new ApiResponse(true, "Product images successfully reordered");
    }

    /**
     * Helper method to handle primary image updates
     */
    private void handlePrimaryImageUpdate(Long productId, Long excludeImageId) {
        imageRepository.updateOtherImagesNonPrimary(productId, excludeImageId != null ? excludeImageId : -1L);
    }

    /**
     * Get all images for a product
     */
    @Transactional(readOnly = true)
    public List<ProductImageResponseDTO> getProductImages(Long productId) {
        List<ProductImages> images = imageRepository.findByProductProductIdOrderByDisplayOrderAsc(productId);
        return imageMapper.toDto(images);
    }

    /**
     * Get primary image for a product
     */
    @Transactional(readOnly = true)
    public ProductImageResponseDTO getPrimaryImage(Long productId) {
        ProductImages primaryImage = imageRepository.findByProductProductIdAndIsPrimaryTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No primary image found for product"));
        return imageMapper.toDto(primaryImage);
    }
}