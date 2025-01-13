package com.example.api_sell_clothes_v1.Utils;

import java.security.SecureRandom;

public class OTPUtils {

    // Hàm để tạo mã OTP có độ dài nhất định
    public static String generateOTP(int length) {
        String characters = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < length; i++) {
            otp.append(characters.charAt(random.nextInt(characters.length())));
        }

        return otp.toString();
    }
}
