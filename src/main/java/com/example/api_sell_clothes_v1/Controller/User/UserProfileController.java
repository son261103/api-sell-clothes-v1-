package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.ChangePasswordDTO;
import com.example.api_sell_clothes_v1.DTO.Auth.ChangePasswordWithOtpDTO;
import com.example.api_sell_clothes_v1.DTO.Auth.UserProfileDTO;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Service.AuthenticationService;
import com.example.api_sell_clothes_v1.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/profiles")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserService userService;
    private final AuthenticationService authService;

    /**
     * Get current user profile
     */
    @GetMapping
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null || userDetails.getUser() == null) {
                log.error("No authenticated user found in request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "No authenticated user found"));
            }

            log.info("Fetching profile for user: {}", userDetails.getUser().getUserId());
            UserProfileDTO profile = authService.getUserProfile(userDetails.getUser().getUserId());
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error fetching user profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error fetching user profile: " + e.getMessage()));
        }
    }

    /**
     * Update user profile
     */
    @PutMapping
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileDTO profileDTO) {
        try {
            // Validate authentication
            if (userDetails == null || userDetails.getUser() == null) {
                log.error("No authenticated user found in profile update request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "No authenticated user found"));
            }

            Long userId = userDetails.getUser().getUserId();
            log.info("Processing profile update for user: {}", userId);

            // Validate date of birth if provided
            if (profileDTO.getDateOfBirth() != null) {
                try {
                    // Try parsing the date to ensure it's valid
                    LocalDate dateOfBirth = null;

                    // Handle multiple date formats
                    try {
                        dateOfBirth = LocalDate.parse(profileDTO.getDateOfBirth().toString());
                    } catch (DateTimeParseException e1) {
                        try {
                            // Try alternate format (yyyy-MM-dd)
                            dateOfBirth = LocalDate.parse(profileDTO.getDateOfBirth().toString(),
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        } catch (DateTimeParseException e2) {
                            log.error("Invalid date format: {}", profileDTO.getDateOfBirth());
                            return ResponseEntity.badRequest()
                                    .body(new ApiResponse(false, "Invalid date format for date of birth"));
                        }
                    }

                    // Check if date is in the future
                    if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
                        log.error("Future date provided for date of birth: {}", dateOfBirth);
                        return ResponseEntity.badRequest()
                                .body(new ApiResponse(false, "Date of birth cannot be in the future"));
                    }

                    // Check if date is too far in the past (e.g., 120+ years ago)
                    LocalDate minValidDate = LocalDate.now().minusYears(120);
                    if (dateOfBirth != null && dateOfBirth.isBefore(minValidDate)) {
                        log.error("Date too far in past: {}", dateOfBirth);
                        return ResponseEntity.badRequest()
                                .body(new ApiResponse(false, "Date of birth is too far in the past"));
                    }

                } catch (Exception e) {
                    log.error("Error validating date of birth: {}", e.getMessage());
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Invalid date format: " + e.getMessage()));
                }
            }

            // Validate phone number format if provided
            if (profileDTO.getPhone() != null && !profileDTO.getPhone().trim().isEmpty()) {
                // Simple validation for numeric phone format
                if (!profileDTO.getPhone().matches("^\\d+$")) {
                    log.error("Invalid phone number format: {}", profileDTO.getPhone());
                    return ResponseEntity.badRequest()
                            .body(new ApiResponse(false, "Invalid phone number format"));
                }
            }

            // Process the update
            try {
                UserProfileDTO updatedProfile = authService.updateUserProfile(userId, profileDTO);
                log.info("Profile updated successfully for user: {}", userId);
                return ResponseEntity.ok(updatedProfile);
            } catch (Exception e) {
                log.error("Error updating profile: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse(false, "Error updating profile: " + e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Unexpected error in profile update: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Unexpected error: " + e.getMessage()));
        }
    }

    /**
     * Change password
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        try {
            // Validate authentication
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            ApiResponse response = authService.changePassword(
                    userDetails.getUser().getUserId(),
                    changePasswordDTO
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse(false, "Error changing password: " + e.getMessage()));
        }
    }

    /**
     * Change password with OTP verification
     */
    @PutMapping("/change-password-otp")
    public ResponseEntity<ApiResponse> changePasswordWithOtp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordWithOtpDTO changePasswordDTO) {
        try {
            // Validate authentication
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            ApiResponse response = authService.changePasswordWithOtp(
                    userDetails.getUser().getUserId(),
                    changePasswordDTO
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse(false, "Error changing password: " + e.getMessage()));
        }
    }

    /**
     * Upload avatar
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("avatar") MultipartFile avatarFile) {
        try {
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            Long userId = userDetails.getUser().getUserId();
            log.info("Uploading avatar for user: {}", userId);
            return ResponseEntity.ok(userService.uploadAvatar(userId, avatarFile));
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error uploading avatar: " + e.getMessage()));
        }
    }

    /**
     * Update avatar
     */
    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("avatar") MultipartFile avatarFile) {
        try {
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            Long userId = userDetails.getUser().getUserId();
            log.info("Updating avatar for user: {}", userId);
            return ResponseEntity.ok(userService.updateAvatar(userId, avatarFile));
        } catch (Exception e) {
            log.error("Error updating avatar: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error updating avatar: " + e.getMessage()));
        }
    }

    /**
     * Delete avatar
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            if (userDetails == null || userDetails.getUser() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "User not authenticated"));
            }

            Long userId = userDetails.getUser().getUserId();
            log.info("Deleting avatar for user: {}", userId);
            return ResponseEntity.ok(userService.deleteAvatar(userId));
        } catch (Exception e) {
            log.error("Error deleting avatar: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Error deleting avatar: " + e.getMessage()));
        }
    }
}