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

    @Transactional
    public void deleteAllUserTokens(Users user) {
        log.info("Deleting all refresh tokens for user: {}", user.getUsername());
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Transactional
    public RefreshTokens createRefreshToken(Users user) {
        log.info("Creating new refresh token for user: {}", user.getUsername());
        try {
            // Xóa tất cả refresh token cũ của user này
            int deletedTokens = refreshTokenRepository.deleteAllByUser(user);
            log.info("Deleted {} old refresh tokens for user: {}", deletedTokens, user.getUsername());

            // Tạo refresh token mới
            RefreshTokens refreshToken = RefreshTokens.builder()
                    .user(user)
                    .refreshToken(UUID.randomUUID().toString())
                    .expirationTime(LocalDateTime.now().plusMonths(1))
                    .build();

            // Lưu và trả về token mới
            RefreshTokens savedToken = refreshTokenRepository.save(refreshToken);
            log.info("Successfully created new refresh token for user: {}", user.getUsername());

            return savedToken;

        } catch (Exception e) {
            log.error("Error creating refresh token for user {}: {}", user.getUsername(), e.getMessage());
            throw new RuntimeException("Failed to create refresh token", e);
        }
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
        log.info("Generating new refresh token to replace old token");
        try {
            // Verify old token
            verifyExpiration(oldToken);

            Users user = oldToken.getUser();

            // Delete old token
            refreshTokenRepository.delete(oldToken);

            // Create new token
            return createRefreshToken(user);

        } catch (Exception e) {
            log.error("Error generating new refresh token: {}", e.getMessage());
            throw new RuntimeException("Failed to generate new refresh token", e);
        }
    }

    public void logout(String refreshToken) {
        log.info("Processing logout with refresh token");
        try {
            // Tìm và xóa refresh token
            RefreshTokens token = refreshTokenRepository.findByRefreshToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            // Xóa tất cả token của user này
            refreshTokenRepository.deleteAllByUser(token.getUser());

            log.info("Successfully logged out user: {}", token.getUser().getUsername());
        } catch (Exception e) {
            log.error("Failed to logout: {}", e.getMessage());
            throw new RuntimeException("Logout failed: " + e.getMessage());
        }
    }

    // Chạy mỗi 24 giờ vào lúc 00:00
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