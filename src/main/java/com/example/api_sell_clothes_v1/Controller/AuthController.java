package com.example.api_sell_clothes_v1.Controller;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.*;
import com.example.api_sell_clothes_v1.Service.AuthenticationService;
import com.example.api_sell_clothes_v1.Service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;
    private final OtpService otpService;

    // Endpoint gửi OTP vào email người dùng
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        try {
            // Kiểm tra xem email có tồn tại trong hệ thống không
            otpService.sendOtpToEmail(email);
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent to your email successfully"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Failed to send OTP: " + e.getMessage()));
        }
    }

    // Endpoint đăng nhập với OTP
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginRequest, @RequestParam(required = false) String otp) {
        try {
            // Kiểm tra xem email là admin@system.com thì không cần OTP
            if (loginRequest.getLoginId().equals("admin@system.com")) {
                TokenResponseDTO tokenResponse = authService.authenticate(loginRequest);
                return ResponseEntity.ok(tokenResponse);
            } else {
                // Kiểm tra và xác thực OTP
                TokenResponseDTO tokenResponse = authService.authenticateWithOTP(loginRequest, otp);
                return ResponseEntity.ok(tokenResponse);
            }
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid username, password or OTP: " + e.getMessage()));
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







    // Endpoint logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Logic xử lý logout (nếu cần)
        return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
    }

}
