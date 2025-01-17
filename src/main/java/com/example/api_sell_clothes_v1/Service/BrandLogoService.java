package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandLogoService {
    private final CloudinaryService cloudinaryService;

    private static final String BRAND_FOLDER = "brands/logos";
    private static final String LOGO_PREFIX = "logo_";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/svg+xml"  // Allow SVG for brand logos
    };

    /**
     * Upload new brand logo
     * @param logoFile The logo file to upload
     * @return URL of the uploaded logo
     * @throws FileHandlingException if upload fails or file validation fails
     */
    public String uploadLogo(MultipartFile logoFile) {
        if (logoFile == null || logoFile.isEmpty()) {
            log.debug("No logo file provided for upload");
            return null;
        }

        validateFile(logoFile);

        try {
            String logoUrl = cloudinaryService.uploadFile(
                    logoFile,
                    BRAND_FOLDER,
                    LOGO_PREFIX
            );
            log.info("Successfully uploaded new brand logo: {}", logoUrl);
            return logoUrl;
        } catch (Exception e) {
            log.error("Failed to upload brand logo: ", e);
            throw new FileHandlingException("Không thể tải lên logo thương hiệu: " + e.getMessage());
        }
    }

    /**
     * Update existing brand logo
     * @param newLogoFile The new logo file
     * @param oldLogoUrl URL of the existing logo
     * @return URL of the updated logo
     * @throws FileHandlingException if update fails or file validation fails
     */
    public String updateLogo(MultipartFile newLogoFile, String oldLogoUrl) {
        if (newLogoFile == null || newLogoFile.isEmpty()) {
            log.debug("No new logo file provided for update");
            return oldLogoUrl;
        }

        validateFile(newLogoFile);

        try {
            // Upload new file first
            String newLogoUrl = cloudinaryService.uploadFile(
                    newLogoFile,
                    BRAND_FOLDER,
                    LOGO_PREFIX
            );

            // Then try to delete old file if exists
            if (oldLogoUrl != null && !oldLogoUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(oldLogoUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old logo, but new logo was uploaded successfully: {}", e.getMessage());
                }
            }

            log.info("Successfully updated brand logo. New URL: {}", newLogoUrl);
            return newLogoUrl;

        } catch (Exception e) {
            log.error("Failed to update brand logo: ", e);
            throw new FileHandlingException("Không thể cập nhật logo thương hiệu: " + e.getMessage());
        }
    }

    /**
     * Delete brand logo
     * @param logoUrl URL of the logo to delete
     * @throws FileHandlingException if deletion fails
     */
    public void deleteLogo(String logoUrl) {
        if (logoUrl == null || logoUrl.isEmpty()) {
            log.debug("No logo URL provided for deletion");
            return;
        }

        try {
            cloudinaryService.deleteFile(logoUrl);
            log.info("Successfully deleted brand logo: {}", logoUrl);
        } catch (Exception e) {
            log.error("Failed to delete brand logo {}: ", logoUrl, e);
            throw new FileHandlingException("Không thể xóa logo thương hiệu: " + e.getMessage());
        }
    }

    /**
     * Validate the logo file
     * @param file The file to validate
     * @throws FileHandlingException if validation fails
     */
    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileHandlingException("Kích thước file vượt quá giới hạn cho phép (2MB)");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new FileHandlingException("Loại file không được hỗ trợ. Chỉ chấp nhận JPEG, PNG, WebP hoặc SVG");
        }

        // Additional validation could be added here (e.g., image dimensions)
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