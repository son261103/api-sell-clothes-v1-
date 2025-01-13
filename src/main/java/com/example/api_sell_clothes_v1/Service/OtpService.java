package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import com.example.api_sell_clothes_v1.Utils.OTPUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.management.relation.Role;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    // Dùng ConcurrentHashMap để lưu trữ OTP (hoặc bạn có thể sử dụng Redis nếu muốn)
    private static final ConcurrentHashMap<String, String> otpCache = new ConcurrentHashMap<>();

    // Phương thức gửi OTP vào email
    public void sendOtpToEmail(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Tạo mã OTP ngẫu nhiên
        String otp = OTPUtils.generateOTP(6);  // Mã OTP 6 chữ số

        // Lưu OTP vào cache để xác thực sau
        otpCache.put(user.getEmail(), otp);

        // Gửi email chứa OTP cho người dùng (dùng emailService)
        emailService.sendOtpWithOTP(user.getEmail(), user.getUsername(), otp);
    }

    // Phương thức lấy OTP đã lưu trong cache
    public String getOtpFromCache(String email) {
        return otpCache.get(email);
    }

    // Phương thức xóa OTP khỏi cache sau khi xác thực thành công
    public void removeOtpFromCache(String email) {
        otpCache.remove(email);
    }
}
