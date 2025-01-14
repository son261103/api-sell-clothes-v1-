package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Utils.OTPUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // Cache để lưu OTP với cấu trúc: <email, OtpData>
    private static final ConcurrentHashMap<String, OtpData> otpCache = new ConcurrentHashMap<>();

    // Thời gian hết hạn OTP (5 phút)
    private static final long OTP_EXPIRATION_MINUTES = 5;

    // Số lần tối đa được gửi OTP trong 24h
    private static final int MAX_OTP_REQUESTS_PER_DAY = 5;

    private static final long RESEND_COOLDOWN_SECONDS = 60;

    // Cache để theo dõi số lần gửi OTP
    private static final ConcurrentHashMap<String, OtpRequestCount> otpRequestCounts = new ConcurrentHashMap<>();

    /**
     * Gửi OTP vào email người dùng
     */
    public void sendOtpToEmail(String email) {
        try {
            log.info("Starting OTP generation for email: {}", email);

            // Kiểm tra email có tồn tại
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // Kiểm tra giới hạn gửi OTP
            checkOtpRequestLimit(email);

            checkResendCooldown(email);

            // Tạo mã OTP mới
            String otp = OTPUtils.generateOTP(6);

            // Lưu OTP vào cache
            saveOtpToCache(email, otp);

            // Gửi OTP qua email
            emailService.sendOtpWithOTP(user.getEmail(), user.getUsername(), otp);

            // Cập nhật số lần gửi OTP
            updateOtpRequestCount(email);

            log.info("OTP sent successfully to email: {}", email);

        } catch (Exception e) {
            log.error("Failed to send OTP to email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra thời gian chờ giữa các lần gửi
     */
    private void checkResendCooldown(String email) {
        OtpRequestCount requestCount = otpRequestCounts.get(email);
        if (requestCount != null && requestCount.getLastRequestTime() != null) {
            long secondsSinceLastRequest = java.time.temporal.ChronoUnit.SECONDS.between(
                    requestCount.getLastRequestTime(),
                    LocalDateTime.now()
            );

            if (secondsSinceLastRequest < RESEND_COOLDOWN_SECONDS) {
                long remainingSeconds = RESEND_COOLDOWN_SECONDS - secondsSinceLastRequest;
                throw new RuntimeException(
                        String.format("Please wait %d seconds before requesting a new OTP", remainingSeconds)
                );
            }
        }
    }

    /**
     * Lấy OTP từ cache
     */
    public String getOtpFromCache(String email) {
        OtpData otpData = otpCache.get(email);
        if (otpData == null || isOtpExpired(otpData.getCreatedAt())) {
            return null;
        }
        return otpData.getOtp();
    }

    /**
     * Xóa OTP khỏi cache
     */
    public void removeOtpFromCache(String email) {
        otpCache.remove(email);
        log.info("OTP removed from cache for email: {}", email);
    }

    /**
     * Kiểm tra giới hạn gửi OTP
     */
    private void checkOtpRequestLimit(String email) {
        OtpRequestCount requestCount = otpRequestCounts.get(email);
        if (requestCount != null) {
            if (requestCount.getCount() >= MAX_OTP_REQUESTS_PER_DAY &&
                    !isNextDay(requestCount.getLastRequestTime())) {
                log.error("OTP request limit exceeded for email: {}", email);
                throw new RuntimeException("OTP request limit exceeded. Please try again tomorrow.");
            }
        }
    }

    /**
     * Cập nhật số lần gửi OTP
     */
    private void updateOtpRequestCount(String email) {
        OtpRequestCount requestCount = otpRequestCounts.get(email);
        if (requestCount == null) {
            requestCount = new OtpRequestCount();
            otpRequestCounts.put(email, requestCount);
        } else if (isNextDay(requestCount.getLastRequestTime())) {
            requestCount.resetCount();
        }
        requestCount.incrementCount();
        requestCount.setLastRequestTime(LocalDateTime.now());
    }

    /**
     * Lưu OTP vào cache
     */
    private void saveOtpToCache(String email, String otp) {
        OtpData otpData = new OtpData(otp, LocalDateTime.now());
        otpCache.put(email, otpData);
        log.info("OTP cached for email: {}", email);
    }

    /**
     * Kiểm tra OTP đã hết hạn chưa
     */
    private boolean isOtpExpired(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        return createdAt.plusMinutes(OTP_EXPIRATION_MINUTES).isBefore(now);
    }

    /**
     * Kiểm tra có phải ngày hôm sau không
     */
    private boolean isNextDay(LocalDateTime time) {
        return time.plusDays(1).isBefore(LocalDateTime.now());
    }

    /**
     * Class lưu trữ thông tin OTP
     */
    @Getter
    private static class OtpData {
        private final String otp;
        private final LocalDateTime createdAt;

        public OtpData(String otp, LocalDateTime createdAt) {
            this.otp = otp;
            this.createdAt = createdAt;
        }

    }

    /**
     * Class theo dõi số lần gửi OTP
     */
    @Getter
    private static class OtpRequestCount {
        private int count;
        private LocalDateTime lastRequestTime;

        public OtpRequestCount() {
            this.count = 0;
            this.lastRequestTime = LocalDateTime.now();
        }

        public void incrementCount() {
            this.count++;
        }

        public void resetCount() {
            this.count = 0;
        }

        public void setLastRequestTime(LocalDateTime lastRequestTime) {
            this.lastRequestTime = lastRequestTime;
        }
    }
}