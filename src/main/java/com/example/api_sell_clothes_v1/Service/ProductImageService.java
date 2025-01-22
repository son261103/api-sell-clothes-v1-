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
import org.springframework.web.multipart.MultipartFile;

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
     * Upload and add multiple product images
     */
    @Transactional
    public List<ProductImageResponseDTO> uploadProductImages(Long productId, List<MultipartFile> files) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if product already has images
        boolean hasExistingImages = imageRepository.countByProductId(productId) > 0;

        List<ProductImages> newImages = new ArrayList<>();
        int order = getNextDisplayOrder(productId);

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // Upload image to Cloudinary
            String imageUrl = cloudinaryService.uploadFile(file, IMAGE_FOLDER, IMAGE_PREFIX);

            ProductImages image = new ProductImages();
            image.setProduct(product);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(order + i);

            // Set first image as primary if no existing images
            image.setIsPrimary(!hasExistingImages && i == 0);

            newImages.add(image);
        }

        List<ProductImages> savedImages = imageRepository.saveAll(newImages);
        log.info("Added {} new product images for product ID: {}", savedImages.size(), productId);

        return imageMapper.toDto(savedImages);
    }

    /**
     * Update image primary status
     */
    @Transactional
    public ProductImageResponseDTO updatePrimaryStatus(Long imageId) {
        ProductImages image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // If image is already primary, do nothing
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            return imageMapper.toDto(image);
        }

        // Set all other images as non-primary
        imageRepository.updateOtherImagesNonPrimary(
                image.getProduct().getProductId(),
                image.getImageId()
        );

        // Set this image as primary
        image.setIsPrimary(true);
        ProductImages savedImage = imageRepository.save(image);

        log.info("Updated primary image for product ID: {} to image ID: {}",
                image.getProduct().getProductId(), imageId);

        return imageMapper.toDto(savedImage);
    }

    /**
     * Reorder product images
     */
    @Transactional
    public ApiResponse reorderProductImages(Long productId, List<Long> imageIds) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<ProductImages> currentImages = imageRepository
                .findByProductProductIdOrderByDisplayOrderAsc(productId);

        // Validate all images belong to the product
        if (currentImages.size() != imageIds.size() ||
                !currentImages.stream()
                        .map(ProductImages::getImageId)
                        .collect(java.util.stream.Collectors.toSet())
                        .containsAll(imageIds)) {
            throw new IllegalArgumentException("Invalid image list provided");
        }

        // Update display orders
        for (int i = 0; i < imageIds.size(); i++) {
            ProductImages image = imageRepository.findById(imageIds.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
            image.setDisplayOrder(i);
            imageRepository.save(image);
        }

        log.info("Reordered images for product ID: {}", productId);
        return new ApiResponse(true, "Images reordered successfully");
    }

    /**
     * Delete product image
     */
    @Transactional
    public ApiResponse deleteProductImage(Long imageId) {
        ProductImages image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        Long productId = image.getProduct().getProductId();

        // If deleting primary image, set next image as primary
        if (Boolean.TRUE.equals(image.getIsPrimary())) {
            List<ProductImages> otherImages = imageRepository
                    .findByProductProductIdOrderByDisplayOrderAsc(productId)
                    .stream()
                    .filter(img -> !img.getImageId().equals(imageId))
                    .toList();

            if (!otherImages.isEmpty()) {
                ProductImages newPrimary = otherImages.get(0);
                newPrimary.setIsPrimary(true);
                imageRepository.save(newPrimary);
            }
        }

        // Delete image file from storage
        try {
            cloudinaryService.deleteFile(image.getImageUrl());
        } catch (Exception e) {
            log.warn("Failed to delete image file: {}", e.getMessage());
        }

        // Delete image record
        imageRepository.delete(image);

        // Reorder remaining images
        reorderImagesAfterDeletion(productId);

        log.info("Deleted product image ID: {} from product ID: {}", imageId, productId);
        return new ApiResponse(true, "Image deleted successfully");
    }

    /**
     * Get all images for a product
     */
    @Transactional(readOnly = true)
    public List<ProductImageResponseDTO> getProductImages(Long productId) {
        List<ProductImages> images = imageRepository
                .findByProductProductIdOrderByDisplayOrderAsc(productId);
        return imageMapper.toDto(images);
    }

    /**
     * Get primary image for a product
     */
    @Transactional(readOnly = true)
    public ProductImageResponseDTO getPrimaryImage(Long productId) {
        ProductImages primaryImage = imageRepository
                .findByProductProductIdAndIsPrimaryTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("No primary image found"));
        return imageMapper.toDto(primaryImage);
    }

    /**
     * Update image file
     */
    @Transactional
    public ProductImageResponseDTO updateImageFile(Long imageId, MultipartFile newFile) {
        ProductImages image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // Upload new image
        String newImageUrl = cloudinaryService.uploadFile(newFile, IMAGE_FOLDER, IMAGE_PREFIX);

        // Delete old image
        try {
            cloudinaryService.deleteFile(image.getImageUrl());
        } catch (Exception e) {
            log.warn("Failed to delete old image file: {}", e.getMessage());
        }

        // Update image URL
        image.setImageUrl(newImageUrl);
        ProductImages savedImage = imageRepository.save(image);

        log.info("Updated image file for image ID: {}", imageId);
        return imageMapper.toDto(savedImage);
    }

    /**
     * Update image properties
     */
    @Transactional
    public ProductImageResponseDTO updateImageProperties(Long imageId, ProductImageUpdateDTO updateDTO) {
        ProductImages image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // If updating to primary, handle other images
        if (Boolean.TRUE.equals(updateDTO.getIsPrimary()) && !Boolean.TRUE.equals(image.getIsPrimary())) {
            imageRepository.updateOtherImagesNonPrimary(
                    image.getProduct().getProductId(),
                    image.getImageId()
            );
        }

        // Update display order if provided
        if (updateDTO.getDisplayOrder() != null) {
            image.setDisplayOrder(updateDTO.getDisplayOrder());
        }

        // Update primary status if provided
        if (updateDTO.getIsPrimary() != null) {
            image.setIsPrimary(updateDTO.getIsPrimary());
        }

        ProductImages savedImage = imageRepository.save(image);
        log.info("Updated properties for image ID: {}", imageId);
        return imageMapper.toDto(savedImage);
    }

    /**
     * Get product images hierarchy information
     */
    @Transactional(readOnly = true)
    public ProductImageHierarchyDTO getProductImagesHierarchy(Long productId) {
        List<ProductImages> images = imageRepository
                .findByProductProductIdOrderByDisplayOrderAsc(productId);
        long primaryCount = images.stream()
                .filter(ProductImages::getIsPrimary)
                .count();

        return ProductImageHierarchyDTO.builder()
                .images(imageMapper.toDto(images))
                .productId(productId)
                .totalImages(images.size())
                .primaryImages((int) primaryCount)
                .nonPrimaryImages(images.size() - (int) primaryCount)
                .build();
    }

    /**
     * Helper method to get next display order for a product
     */
    private int getNextDisplayOrder(Long productId) {
        return imageRepository.findMaxDisplayOrderByProductId(productId)
                .map(order -> order + 1)
                .orElse(0);
    }

    /**
     * Helper method to reorder images after deletion
     */
    private void reorderImagesAfterDeletion(Long productId) {
        List<ProductImages> remainingImages = imageRepository
                .findByProductProductIdOrderByDisplayOrderAsc(productId);

        for (int i = 0; i < remainingImages.size(); i++) {
            ProductImages image = remainingImages.get(i);
            image.setDisplayOrder(i);
            imageRepository.save(image);
        }
    }
}