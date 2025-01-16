package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Exceptions.FileHandlingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarService {
    private final CloudinaryService cloudinaryService;

    private static final String USER_FOLDER = "users/avatars";
    private static final String AVATAR_PREFIX = "avatar_";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg",
            "image/png",
            "image/gif"
    };

    /**
     * Upload new avatar
     * @param avatarFile The avatar file to upload
     * @return URL of the uploaded avatar
     * @throws FileHandlingException if upload fails or file validation fails
     */
    public String uploadAvatar(MultipartFile avatarFile) {
        if (avatarFile == null || avatarFile.isEmpty()) {
            log.debug("No avatar file provided for upload");
            return null;
        }

        validateFile(avatarFile);

        try {
            String avatarUrl = cloudinaryService.uploadFile(
                    avatarFile,
                    USER_FOLDER,
                    AVATAR_PREFIX
            );
            log.info("Successfully uploaded new avatar: {}", avatarUrl);
            return avatarUrl;
        } catch (Exception e) {
            log.error("Failed to upload avatar: ", e);
            throw new FileHandlingException("Không thể tải lên avatar: " + e.getMessage());
        }
    }

    /**
     * Update existing avatar
     * @param newAvatarFile The new avatar file
     * @param oldAvatarUrl URL of the existing avatar
     * @return URL of the updated avatar
     * @throws FileHandlingException if update fails or file validation fails
     */
    public String updateAvatar(MultipartFile newAvatarFile, String oldAvatarUrl) {
        if (newAvatarFile == null || newAvatarFile.isEmpty()) {
            log.debug("No new avatar file provided for update");
            return oldAvatarUrl;
        }

        validateFile(newAvatarFile);

        try {
            // Upload new file first
            String newAvatarUrl = cloudinaryService.uploadFile(
                    newAvatarFile,
                    USER_FOLDER,
                    AVATAR_PREFIX
            );

            // Then try to delete old file if exists
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteFile(oldAvatarUrl);
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar, but new avatar was uploaded successfully: {}", e.getMessage());
                    // Don't throw exception here since new file is already uploaded
                }
            }

            log.info("Successfully updated avatar. New URL: {}", newAvatarUrl);
            return newAvatarUrl;

        } catch (Exception e) {
            log.error("Failed to update avatar: ", e);
            throw new FileHandlingException("Không thể cập nhật avatar: " + e.getMessage());
        }
    }

    /**
     * Delete avatar
     * @param avatarUrl URL of the avatar to delete
     * @throws FileHandlingException if deletion fails
     */
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            log.debug("No avatar URL provided for deletion");
            return;
        }

        try {
            cloudinaryService.deleteFile(avatarUrl);
            log.info("Successfully deleted avatar: {}", avatarUrl);
        } catch (Exception e) {
            log.error("Failed to delete avatar {}: ", avatarUrl, e);
            throw new FileHandlingException("Không thể xóa avatar: " + e.getMessage());
        }
    }

    /**
     * Validate the avatar file
     * @param file The file to validate
     * @throws FileHandlingException if validation fails
     */
    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileHandlingException("Kích thước file vượt quá giới hạn cho phép (5MB)");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new FileHandlingException("Loại file không được hỗ trợ. Chỉ chấp nhận JPEG, PNG hoặc GIF");
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
