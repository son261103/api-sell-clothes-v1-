package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.*;
import com.example.api_sell_clothes_v1.Entity.RefreshTokens;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Repository.RoleRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AuthorityService authorityService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // Phương thức xác thực người dùng thông thường
    public TokenResponseDTO authenticate(LoginDTO request) {
        log.info("Attempting authentication for user: {}", request.getLoginId());
        try {
            // Thực hiện xác thực
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    )
            );

            // Tìm user từ database
            Users user = userRepository.findByLoginId(request.getLoginId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Tạo custom user details
            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            // Tạo tokens
            String accessToken = jwtService.generateToken(userDetails);
            RefreshTokens refreshToken = refreshTokenService.createRefreshToken(user);

            // Lấy roles và permissions
            Set<String> roles = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toSet());

            Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getCodeName())
                    .collect(Collectors.toSet());

            log.info("Authentication successful for user: {}", request.getLoginId());

            // Trả về token response
            return TokenResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getLoginId());

            // Kiểm tra và log chi tiết lỗi nhưng vẫn trả về thông báo chung
            Users user = userRepository.findByLoginId(request.getLoginId()).orElse(null);
            if (user != null && user.getStatus() != UserStatus.ACTIVE) {
                log.error("User status is: {}", user.getStatus());
            }

            // Luôn trả về thông báo chung cho mọi trường hợp lỗi
            throw new BadCredentialsException("Invalid username/password");
        }
    }



    // Phương thức đăng ký người dùng
    @Transactional
    public RegisterResponseDTO register(RegisterDTO registerDTO) {
        log.info("Starting registration process for email: {}", registerDTO.getEmail());
        try {
            // Validate đầu vào
            validateRegistrationInput(registerDTO);

            // Kiểm tra email và username tồn tại
            if (userRepository.existsByEmail(registerDTO.getEmail())) {
                throw new RuntimeException("Email already exists");
            }

            if (userRepository.existsByUsername(registerDTO.getUsername())) {
                throw new RuntimeException("Username already exists");
            }

            // Mã hóa mật khẩu
            String encodedPassword = passwordEncoder.encode(registerDTO.getPassword());

            // Tạo user mới
            Users newUser = Users.builder()
                    .username(registerDTO.getUsername())
                    .email(registerDTO.getEmail())
                    .passwordHash(encodedPassword)
                    .fullName(registerDTO.getFullName())
                    .phone(registerDTO.getPhone())
                    .status(UserStatus.PENDING)
                    .roles(new HashSet<>())
                    .build();

            // Thêm ROLE_CUSTOMER mặc định
            Roles userRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Default role not found"));
            newUser.getRoles().add(userRole);

            // Lưu user
            Users savedUser = userRepository.save(newUser);

            log.info("User registered successfully: {}", registerDTO.getEmail());

            // Trả về response
            return RegisterResponseDTO.builder()
                    .userId(savedUser.getUserId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .message("Registration successful. Please verify your email.")
                    .requiresEmailVerification(true)
                    .build();

        } catch (Exception e) {
            log.error("Registration failed for email: {}", registerDTO.getEmail(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    // Phương thức xác thực OTP cho đăng ký
    public boolean verifyOtpForRegister(String email, String otp) {
        log.info("Verifying OTP for email: {}", email);
        try {
            // Kiểm tra OTP từ cache
            String cachedOtp = otpService.getOtpFromCache(email);
            if (cachedOtp == null || !cachedOtp.equals(otp)) {
                log.error("Invalid OTP for email: {}", email);
                return false;
            }

            // Tìm user
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Cập nhật trạng thái
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            // Xóa OTP
            otpService.removeOtpFromCache(email);

            log.info("OTP verification successful for email: {}", email);
            return true;

        } catch (Exception e) {
            log.error("OTP verification failed for email: {}", email, e);
            return false;
        }
    }

    public ApiResponse resendOtp(String email) {
        log.info("Processing resend OTP request for email: {}", email);
        try {
            // Kiểm tra email tồn tại
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Kiểm tra trạng thái user
            if (user.getStatus() == UserStatus.ACTIVE) {
                throw new RuntimeException("User is already verified");
            }

            if (user.getStatus() == UserStatus.BANNED){
                throw new RuntimeException("account has been banned");
            }

            if (user.getStatus() == UserStatus.LOCKED){
                throw new RuntimeException("account has been locked");
            }

            // Xóa OTP cũ nếu có
            otpService.removeOtpFromCache(email);

            // Gửi OTP mới
            otpService.sendOtpToEmail(email);

            log.info("Successfully resent OTP to email: {}", email);
            return new ApiResponse(true, "New OTP has been sent to your email");

        } catch (Exception e) {
            log.error("Failed to resend OTP for email {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to resend OTP: " + e.getMessage());
        }
    }

    public ApiResponse initiateForgotPassword(ForgotPasswordDTO forgotPasswordDTO) {
        log.info("Initiating forgot password for loginId: {}", forgotPasswordDTO.getLoginId());
        try {
            // Kiểm tra user tồn tại bằng loginId
            Users user = userRepository.findByLoginId(forgotPasswordDTO.getLoginId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Gửi OTP qua email
            otpService.sendOtpToEmail(user.getEmail());

            return new ApiResponse(true, "Password reset OTP has been sent to your email");
        } catch (Exception e) {
            log.error("Failed to initiate forgot password for loginId: {}", forgotPasswordDTO.getLoginId(), e);
            throw new RuntimeException("Failed to initiate password reset: " + e.getMessage());
        }
    }

    public TokenResponseDTO refreshToken(RefreshTokenDTO refreshTokenDTO) {
        log.info("Processing refresh token request");
        try {
            // 1. Tìm refresh token trong database
            RefreshTokens storedToken = refreshTokenService.findByToken(refreshTokenDTO.getRefreshToken())
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            // 2. Kiểm tra token còn hạn
            refreshTokenService.verifyExpiration(storedToken);

            // 3. Tìm user - Updated to use getUser() instead of getUserId()
            Users user = storedToken.getUser(); // Changed this line since user is already linked
            if (user == null) {
                throw new RuntimeException("User not found");
            }

            // 4. Kiểm tra trạng thái user
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new DisabledException("User account is not active");
            }

            // 5. Tạo custom user details
            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            // 6. Tạo access token mới
            String accessToken = jwtService.generateToken(userDetails);

            // 7. Tạo refresh token mới và lưu vào database
            RefreshTokens newRefreshToken = refreshTokenService.generateNewRefreshToken(storedToken);

            // 8. Lấy roles và permissions
            Set<String> roles = jwtService.extractRoles(accessToken);
            Set<String> permissions = jwtService.extractPermissions(accessToken);

            log.info("Token refresh successful for user: {}", user.getUsername());

            // 9. Trả về response
            return TokenResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roles(roles)
                    .permissions(permissions)
                    .build();

        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token: " + e.getMessage());
        }
    }

    public ApiResponse resetPassword(ResetPasswordDTO resetPasswordDTO) {
        log.info("Processing password reset for email: {}", resetPasswordDTO.getEmail());
        try {
            // Validate đầu vào
            if (resetPasswordDTO.getNewPassword() == null || resetPasswordDTO.getNewPassword().trim().isEmpty()) {
                throw new RuntimeException("New password is required");
            }

            if (resetPasswordDTO.getConfirmPassword() == null || resetPasswordDTO.getConfirmPassword().trim().isEmpty()) {
                throw new RuntimeException("Confirm password is required");
            }

            if (!resetPasswordDTO.getNewPassword().equals(resetPasswordDTO.getConfirmPassword())) {
                throw new RuntimeException("New password and confirm password do not match");
            }

            // Verify OTP
            String cachedOtp = otpService.getOtpFromCache(resetPasswordDTO.getEmail());
            if (cachedOtp == null || !cachedOtp.equals(resetPasswordDTO.getOtp())) {
                throw new RuntimeException("Invalid or expired OTP");
            }

            // Tìm user
            Users user = userRepository.findByEmail(resetPasswordDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Cập nhật mật khẩu mới
            user.setPasswordHash(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
            userRepository.save(user);

            // Xóa OTP sau khi reset thành công
            otpService.removeOtpFromCache(resetPasswordDTO.getEmail());

            log.info("Password reset successful for email: {}", resetPasswordDTO.getEmail());
            return new ApiResponse(true, "Your password has been reset successfully");

        } catch (Exception e) {
            log.error("Failed to reset password for email: {}", resetPasswordDTO.getEmail(), e);
            throw new RuntimeException("Failed to reset password: " + e.getMessage());
        }
    }

    // Helper method để validate input
    private void validateRegistrationInput(RegisterDTO registerDTO) {
        if (registerDTO.getEmail() == null || registerDTO.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (registerDTO.getPassword() == null || registerDTO.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
    }

    public ApiResponse logout(String refreshToken) {
        log.info("Processing logout request");
        try {
            refreshTokenService.logout(refreshToken);
            return new ApiResponse(true, "Logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }
}