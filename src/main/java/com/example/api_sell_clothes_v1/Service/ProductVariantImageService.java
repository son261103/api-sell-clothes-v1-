package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVariantImageService {
    private final CloudinaryService cloudinaryService;

    private static final String VARIANT_FOLDER = "products/variants";
    private static final String IMAGE_PREFIX = "variant_";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp"
    };

    /**
     * Upload new variant image
     */
    public String uploadImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            log.debug("No variant image file provided for upload");
            return null;
        }

        validateFile(imageFile);

        try {
            String imageUrl = cloudinaryService.uploadFile(
                    imageFile,
                    VARIANT_FOLDER,
                    IMAGE_PREFIX
            );
            log.info("Successfully uploaded new variant image: {}", imageUrl);
            return imageUrl;
        } catch (Exception e) {
            log.error("Failed to upload variant image: ", e);
            throw new FileHandlingException("Cannot upload variant image: " + e.getMessage());
        }
    }

    /**
     * Update existing variant image
     */
    public String updateImage(MultipartFile newImageFile, String oldImageUrl) {
        if (newImageFile == null || newImageFile.isEmpty()) {
            log.debug("No new variant image file provided for update");
            return oldImageUrl;
        }

        validateFile(newImageFile);

        try {
            // Upload new file first
            String newImageUrl = cloudinaryService.uploadFile(
                    newImageFile,
                    VARIANT_FOLDER,
                    IMAGE_PREFIX
            );

            // Then try to delete old file if exists
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(oldImageUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old variant image, but new image was uploaded successfully: {}", e.getMessage());
                }
            }

            log.info("Successfully updated variant image. New URL: {}", newImageUrl);
            return newImageUrl;

        } catch (Exception e) {
            log.error("Failed to update variant image: ", e);
            throw new FileHandlingException("Unable to update variant image: " + e.getMessage());
        }
    }

    /**
     * Delete variant image
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            log.debug("No variant image URL provided for deletion");
            return;
        }

        try {
            cloudinaryService.deleteFile(imageUrl);
            log.info("Successfully deleted variant image: {}", imageUrl);
        } catch (Exception e) {
            log.error("Failed to delete variant image {}: ", imageUrl, e);
            throw new FileHandlingException("Variant image cannot be deleted: " + e.getMessage());
        }
    }




    /**
     * Validate the image file
     */
    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileHandlingException("File size exceeds the allowed limit (5MB)");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new FileHandlingException("File type not supported. Only JPEG, PNG or WebP are accepted");
        }
    }

    /**
     * Check if the content type is allowed
     */
    private boolean isAllowedContentType(String contentType) {
        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }
}