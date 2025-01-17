package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Auth.*;
import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Entity.RefreshTokens;
import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.example.api_sell_clothes_v1.Exceptions.UserStatusException;
import com.example.api_sell_clothes_v1.Repository.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository;

    // Phương thức xác thực người dùng thông thường
    public TokenResponseDTO authenticate(LoginDTO request) {
        log.info("Attempting authentication for user: {}", request.getLoginId());
        try {
            // Kiểm tra user và status trước khi xác thực
            Users user = userRepository.findByLoginId(request.getLoginId())
                    .orElseThrow(() -> new BadCredentialsException("Invalid username/password"));

            // Kiểm tra status và trả về thông báo cụ thể
            switch (user.getStatus()) {
                case UserStatus.PENDING:
                    log.error("User account is inactive: {}", request.getLoginId());
                    throw new UserStatusException("Account has not been activated. Please check your email to activate your account.");
                case UserStatus.BANNED:
                    log.error("User account is banned: {}", request.getLoginId());
                    throw new UserStatusException("The account has been permanently locked. Please contact admin for more details.");
                case UserStatus.LOCKED:
                    log.error("User account is locked: {}", request.getLoginId());
                    throw new UserStatusException("Account temporarily locked. Please try again later or contact admin.");
                case ACTIVE:
                    break;
                default:
                    throw new UserStatusException("Invalid account status.");
            }

            // Thực hiện xác thực password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLoginId(),
                            request.getPassword()
                    )
            );

            // Tạo custom user details
            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            // Xóa tất cả refresh token cũ của user này trước khi tạo mới
            log.info("Deleting all existing refresh tokens for user: {}", user.getUsername());
            refreshTokenService.deleteAllUserTokens(user);

            // Tạo tokens mới
            log.info("Generating new tokens for user: {}", user.getUsername());
            String accessToken = jwtService.generateToken(userDetails);
            RefreshTokens refreshToken = refreshTokenService.createRefreshToken(user);

            // Lấy roles và permissions
            Set<String> roles = user.getRoles().stream()
                    .map(Roles::getName)
                    .collect(Collectors.toSet());

            Set<String> permissions = user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(Permissions::getCodeName)
                    .collect(Collectors.toSet());

            log.info("Authentication successful for user: {}", request.getLoginId());
            log.debug("User roles: {}", roles);
            log.debug("User permissions: {}", permissions);

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

        } catch (UserStatusException e) {
            // Trả về thông báo cụ thể về status
            log.error("User status exception for user {}: {}", request.getLoginId(), e.getMessage());
            throw e;
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.getLoginId());
            throw new BadCredentialsException("Thông tin đăng nhập không chính xác");
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getLoginId());
            throw new BadCredentialsException("Thông tin đăng nhập không chính xác");
        } catch (Exception e) {
            log.error("Unexpected error during authentication for user {}: {}", request.getLoginId(), e.getMessage());
            throw new RuntimeException("Authentication failed due to an unexpected error");
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

            if (user.getStatus() == UserStatus.BANNED) {
                throw new RuntimeException("account has been banned");
            }

            if (user.getStatus() == UserStatus.LOCKED) {
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

    public TokenResponseDTO verifyNewAccess(RefreshTokenDTO refreshTokenDTO) {
        log.info("Processing refresh token request");
        try {
            // 1. Verify refresh token
            if (!refreshTokenService.verifyRefreshTokenForNewAccess(refreshTokenDTO.getRefreshToken())) {
                throw new RuntimeException("Invalid refresh token");
            }

            // 2. Lấy user từ refresh token
            RefreshTokens storedToken = refreshTokenRepository.findByRefreshToken(refreshTokenDTO.getRefreshToken()).get();
            Users user = storedToken.getUser();

            // 3. Tạo custom user details
            CustomUserDetails userDetails = new CustomUserDetails(user, authorityService.getAuthorities(user));

            // 4. Tạo access token mới
            String newAccessToken = jwtService.generateToken(userDetails);

            // 5. Build response với refresh token cũ
            return TokenResponseDTO.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(refreshTokenDTO.getRefreshToken()) // Giữ nguyên refresh token cũ
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getJwtExpiration())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .roles(jwtService.extractRoles(newAccessToken))
                    .permissions(jwtService.extractPermissions(newAccessToken))
                    .build();

        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token: " + e.getMessage());
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