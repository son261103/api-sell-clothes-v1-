package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductThumbnailService {
    private final CloudinaryService cloudinaryService;

    private static final String PRODUCT_FOLDER = "products/thumbnails";
    private static final String THUMBNAIL_PREFIX = "thumbnail_";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif",  // Allow AVIF for thumbnail images
            "image/heif"  // Allow HEIF for thumbnail images
    };

    /**
     * Upload new product thumbnail
     * @param thumbnailFile The thumbnail file to upload
     * @return URL of the uploaded thumbnail
     * @throws FileHandlingException if upload fails or file validation fails
     */
    public String uploadThumbnail(MultipartFile thumbnailFile) {
        if (thumbnailFile == null || thumbnailFile.isEmpty()) {
            log.debug("No thumbnail file provided for upload");
            return null;
        }

        validateFile(thumbnailFile);

        try {
            String thumbnailUrl = cloudinaryService.uploadFile(
                    thumbnailFile,
                    PRODUCT_FOLDER,
                    THUMBNAIL_PREFIX
            );
            log.info("Successfully uploaded new product thumbnail: {}", thumbnailUrl);
            return thumbnailUrl;
        } catch (Exception e) {
            log.error("Failed to upload product thumbnail: ", e);
            throw new FileHandlingException("Không thể tải lên ảnh thumbnail sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Update existing product thumbnail
     * @param newThumbnailFile The new thumbnail file
     * @param oldThumbnailUrl URL of the existing thumbnail
     * @return URL of the updated thumbnail
     * @throws FileHandlingException if update fails or file validation fails
     */
    public String updateThumbnail(MultipartFile newThumbnailFile, String oldThumbnailUrl) {
        if (newThumbnailFile == null || newThumbnailFile.isEmpty()) {
            log.debug("No new thumbnail file provided for update");
            return oldThumbnailUrl;
        }

        validateFile(newThumbnailFile);

        try {
            // Upload new file first
            String newThumbnailUrl = cloudinaryService.uploadFile(
                    newThumbnailFile,
                    PRODUCT_FOLDER,
                    THUMBNAIL_PREFIX
            );

            // Then try to delete old file if exists
            if (oldThumbnailUrl != null && !oldThumbnailUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(oldThumbnailUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old thumbnail, but new thumbnail was uploaded successfully: {}", e.getMessage());
                }
            }

            log.info("Successfully updated product thumbnail. New URL: {}", newThumbnailUrl);
            return newThumbnailUrl;

        } catch (Exception e) {
            log.error("Failed to update product thumbnail: ", e);
            throw new FileHandlingException("Unable to update product thumbnail image: " + e.getMessage());
        }
    }

    /**
     * Delete product thumbnail
     * @param thumbnailUrl URL of the thumbnail to delete
     * @throws FileHandlingException if deletion fails
     */
    public void deleteThumbnail(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
            log.debug("No thumbnail URL provided for deletion");
            return;
        }

        try {
            cloudinaryService.deleteFile(thumbnailUrl);
            log.info("Successfully deleted product thumbnail: {}", thumbnailUrl);
        } catch (Exception e) {
            log.error("Failed to delete product thumbnail {}: ", thumbnailUrl, e);
            throw new FileHandlingException("Product thumbnails cannot be deleted: " + e.getMessage());
        }
    }

    /**
     * Validate the thumbnail file
     * @param file The file to validate
     * @throws FileHandlingException if validation fails
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

        // Additional validation could be added here (e.g., minimum image dimensions)
    }

    /**
     * Check if the content type is allowed
     * @param contentType The content type to check
     * @return true if allowed, false otherwise
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