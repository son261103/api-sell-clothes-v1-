package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.RefreshTokens;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Repository.RefreshTokenRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public RefreshTokens createRefreshToken(Users user) {
        // Xóa refresh token cũ nếu có
        refreshTokenRepository.findByUser(user)
                .ifPresent(token -> refreshTokenRepository.delete(token));

        // Tạo refresh token mới
        RefreshTokens refreshToken = RefreshTokens.builder()
                .user(user) // Changed from userId
                .refreshToken(UUID.randomUUID().toString())
                .expirationTime(LocalDateTime.now().plusMonths(1))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshTokens verifyExpiration(RefreshTokens token) {
        if (token.getExpirationTime().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new login request");
        }
        return token;
    }

    public Optional<RefreshTokens> findByToken(String token) {
        return refreshTokenRepository.findByRefreshToken(token);
    }

    public RefreshTokens generateNewRefreshToken(RefreshTokens oldToken) {
        // Verify old token
        verifyExpiration(oldToken);

        // Find user
        Users user = userRepository.findById(oldToken.getUser().getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete old token
        refreshTokenRepository.delete(oldToken);

        // Create new token
        return createRefreshToken(user);
    }

    public void logout(String refreshToken) {
        log.info("Processing logout with refresh token");
        try {
            // Tìm và xóa refresh token
            RefreshTokens token = refreshTokenRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            // Xóa token
            refreshTokenRepository.delete(token);

            log.info("Successfully logged out user: {}", token.getUser().getUsername());
        } catch (Exception e) {
            log.error("Failed to logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    // Hoặc sử dụng fixed rate (chạy mỗi 24 giờ)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteExpiredTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens");
        try {
            // Tìm và xóa tất cả token đã hết hạn
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            int deletedCount = refreshTokenRepository.deleteByExpirationTimeBefore(oneMonthAgo);

            log.info("Successfully deleted {} expired refresh tokens", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup expired tokens: {}", e.getMessage());
        }
    }
}