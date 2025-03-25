package com.example.api_sell_clothes_v1.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.api_sell_clothes_v1.Exceptions.CloudinaryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    // Constants for file validation
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "avif");
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("/v\\d+/(.+?)\\.[^.]+$");
    private static final String RESOURCE_TYPE = "image";

    /**
     * Upload file to specific folder
     */
    public String uploadFile(MultipartFile file, String folder, String filePrefix) {
        validateFile(file);
        try {
            String uniqueFileName = filePrefix + UUID.randomUUID().toString();
            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", uniqueFileName,
                    "folder", folder,
                    "quality", "auto",
                    "fetch_format", "auto"
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
            log.info("Successfully uploaded file to folder: {}", folder);
            return uploadResult.get("url").toString();
        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new CloudinaryException("Lỗi khi tải file lên", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete file using Cloudinary URL
     */
    public void deleteFile(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
            log.warn("Empty URL provided for deletion");
            return;
        }

        try {
            String publicId = extractPublicIdFromUrl(cloudinaryUrl);
            if (publicId == null) {
                log.warn("Could not extract public ID from URL: {}", cloudinaryUrl);
                return;
            }

            Map<String, Object> options = ObjectUtils.asMap("resource_type", RESOURCE_TYPE);
            Map result = cloudinary.uploader().destroy(publicId, options);

            if ("ok".equals(result.get("result"))) {
                log.info("Successfully deleted file from Cloudinary. Public ID: {}", publicId);
            } else {
                log.warn("File deletion returned unexpected result: {}", result);
                throw new CloudinaryException("Không thể xóa file: " + result.get("result"), HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage());
            throw new CloudinaryException("Lỗi khi xóa file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            // Handle both http and https URLs
            if (!url.contains("/upload/")) {
                return null;
            }

            Matcher matcher = PUBLIC_ID_PATTERN.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}. Error: {}", url, e.getMessage());
            return null;
        }
    }


    /**
     * Update file - delete old file and upload new one
     */
    public String updateFile(MultipartFile newFile, String oldFileUrl, String folder, String filePrefix) {
        if (oldFileUrl != null && !oldFileUrl.isEmpty()) {
            deleteFile(oldFileUrl);
        }
        return uploadFile(newFile, folder, filePrefix);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CloudinaryException("File không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new CloudinaryException(
                    String.format("Kích thước file vượt quá giới hạn %dMB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new CloudinaryException(
                    String.format("Định dạng file không hợp lệ. Chấp nhận: %s",
                            String.join(", ", ALLOWED_EXTENSIONS))
            );
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            throw new CloudinaryException("Tên file không hợp lệ");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

}