package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.*;
import com.example.api_sell_clothes_v1.Exceptions.UserStatusException;
import com.example.api_sell_clothes_v1.Service.AuthenticationService;
import com.example.api_sell_clothes_v1.Service.EmailService;
import com.example.api_sell_clothes_v1.Service.OtpService;
import com.example.api_sell_clothes_v1.Service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final AuthenticationService authService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService ;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.getLoginId());
        try {
            TokenResponseDTO tokenResponse = authService.authenticate(loginRequest);
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
                    .status(HttpStatus.UNAUTHORIZED)
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
    public ResponseEntity<?> verifyAccessToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        try {
            TokenResponseDTO tokenResponse = authService.verifyNewAccess(refreshTokenDTO);
            return ResponseEntity.ok(tokenResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

//    @PostMapping("/refresh-token")
//    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
//        try {
//            TokenResponseDTO tokenResponse = authService.refreshToken(refreshTokenDTO);
//            return ResponseEntity.ok(tokenResponse);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(new ApiResponse(false, e.getMessage()));
//        }
//    }

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
    public ResponseEntity<?> logout(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        try {
            ApiResponse response = authService.logout(refreshTokenDTO.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Logout failed: " + e.getMessage()));
        }
    }

}
