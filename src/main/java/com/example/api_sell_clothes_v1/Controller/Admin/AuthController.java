package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.*;
import com.example.api_sell_clothes_v1.Exceptions.UserStatusException;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationService authService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;

    // Endpoint gửi OTP vào email người dùng
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        try {
            // Kiểm tra có phải admin@system.com không
            if (email.equals("admin@system.com")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "System admin doesn't need OTP"));
            }

            otpService.sendOtpToEmail(email);
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to send OTP: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        log.info("Verifying OTP for email: {} with OTP: {}", email, otp);
        try {
            boolean response = authService.verifyOtpForRegister(email, otp);
            log.info("OTP verification result: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("OTP verification failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "OTP verification failed: " + e.getMessage()));
        }
    }


    // Endpoint đăng nhập với OTP
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO loginRequest,
            HttpServletResponse response) {
        log.info("Login attempt for user: {}", loginRequest.getLoginId());
        try {
            TokenResponseDTO tokenResponse = authService.authenticate(loginRequest);

            // Chỉ set cookie khi rememberMe là true
            if (loginRequest.isRememberMe()) {
                Cookie refreshTokenCookie = createRefreshTokenCookie(tokenResponse.getRefreshToken());
                response.addCookie(refreshTokenCookie);
                // Remove refresh token from response body when using cookie
                tokenResponse.setRefreshToken(null);
                log.info("Remember me enabled - Set refresh token cookie for user: {}", loginRequest.getLoginId());
            } else {
                // Không set cookie khi không dùng remember me
                // Giữ refresh token trong response body để frontend lưu vào sessionStorage
                log.info("Remember me disabled - Sending refresh token in response body for user: {}", loginRequest.getLoginId());
            }

            log.info("Login successful for user: {}", loginRequest.getLoginId());
            return ResponseEntity.ok(tokenResponse);

        } catch (BadCredentialsException e) {
            log.error("Login failed - Bad credentials for user: {}", loginRequest.getLoginId());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Thông tin đăng nhập không chính xác"));
        } catch (UserStatusException e) {
            log.error("Login failed - Account status issue for user: {}", loginRequest.getLoginId());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Login failed - Unexpected error for user {}: {}", loginRequest.getLoginId(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Đã xảy ra lỗi trong quá trình đăng nhập"));
        }
    }


    // Endpoint đăng ký người dùng
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO, @RequestParam(required = false) String otp) {
        try {
            // Nếu cần OTP để đăng ký
            if (otp != null) {
                // Kiểm tra OTP trước khi thực hiện đăng ký
                boolean otpVerified = authService.verifyOtpForRegister(registerDTO.getEmail(), otp);
                if (!otpVerified) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiResponse(false, "Invalid OTP or OTP expired"));
                }
            }

            // Thực hiện đăng ký người dùng
            RegisterResponseDTO response = authService.register(registerDTO);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Registration failed: " + e.getMessage()));
        }
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordDTO forgotPasswordDTO) {
        try {
            ApiResponse response = authService.initiateForgotPassword(forgotPasswordDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordDTO resetPasswordDTO) {
        try {
            ApiResponse response = authService.resetPassword(resetPasswordDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> verifyAccessToken(HttpServletRequest request) {
        try {
            // Lấy refresh token từ cookie
            Cookie[] cookies = request.getCookies();
            String refreshToken = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }

            // Kiểm tra refresh token trong request body nếu không có trong cookie
            if (refreshToken == null && request.getContentType() != null &&
                    request.getContentType().contains("application/json")) {
                try {
                    // Read refresh token from request body if available
                    RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
                    String requestBody = request.getReader().lines().reduce("", (accumulator, actual) -> accumulator + actual);

                    if (requestBody != null && !requestBody.isEmpty()) {
                        // Simple extraction for "refreshToken" field from JSON
                        int startIndex = requestBody.indexOf("\"refreshToken\":\"");
                        if (startIndex > -1) {
                            startIndex += "\"refreshToken\":\"".length();
                            int endIndex = requestBody.indexOf("\"", startIndex);
                            if (endIndex > -1) {
                                refreshToken = requestBody.substring(startIndex, endIndex);
                                log.info("Found refresh token in request body");
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parsing request body for refresh token: {}", e.getMessage());
                }
            }

            // Kiểm tra refresh token có tồn tại không
            if (refreshToken == null) {
                log.warn("No refresh token found in cookie or request body");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "No refresh token found"));
            }

            // Tạo DTO từ refresh token lấy được
            RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
            refreshTokenDTO.setRefreshToken(refreshToken);

            // Gọi service để verify và tạo token mới
            TokenResponseDTO tokenResponse = authService.verifyNewAccess(refreshTokenDTO);

            // Log token mới được tạo
            log.info("New access token created successfully");

            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            log.error("Error during token refresh: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        try {
            ApiResponse response = authService.resendOtp(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // Lấy refresh token từ cookie
            String refreshToken = extractRefreshTokenFromCookie(request);
            if (refreshToken != null) {
                // Xóa refresh token ở server
                authService.logout(refreshToken);

                // Xóa cookie refresh token
                Cookie cookie = createRefreshTokenCookie("");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }

            return ResponseEntity.ok(new ApiResponse(true, "Đăng xuất thành công"));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Đăng xuất thất bại: " + e.getMessage()));
        }
    }

    // Helper methods
    private Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Enable on HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "Strict");
        return cookie;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


//    -------------------------------------------------------------------------------

    /**
     * Change password without OTP
     */
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        try {
            // Thêm null check
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
            // Thêm null check
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
     * Get user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
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
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileDTO profileDTO) {
        try {
            // Validate authentication
            if (userDetails == null || userDetails.getUser() == null) {
                log.error("No authenticated user found in profile update request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "No authenticated user found"));
            }

            log.info("Processing profile update for user: {}", userDetails.getUser().getUserId());

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
                UserProfileDTO updatedProfile = authService.updateUserProfile(
                        userDetails.getUser().getUserId(),
                        profileDTO
                );
                log.info("Profile updated successfully for user: {}", userDetails.getUser().getUserId());
                return ResponseEntity.ok(updatedProfile);
            } catch (Exception e) {
                log.error("Error in service layer while updating profile: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse(false, "Error updating profile: " + e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Unexpected error in profile update controller: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Unexpected error: " + e.getMessage()));
        }
    }
}