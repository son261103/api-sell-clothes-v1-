package com.example.api_sell_clothes_v1.Config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Getter
public class VnpayConfig {
    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.payment-url}")
    private String paymentUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    private final String version = "2.1.0"; // Phiên bản mặc định của VNPay

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(Map<String, String> vnpParams) {
        try {
            // Thêm các tham số bắt buộc
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(calendar.getTime());

            vnpParams.put("vnp_Version", version);
            vnpParams.put("vnp_TmnCode", tmnCode);
            vnpParams.put("vnp_CreateDate", vnpCreateDate);
            vnpParams.put("vnp_ReturnUrl", returnUrl);
            vnpParams.put("vnp_IpAddr", "127.0.0.1"); // Có thể thay đổi theo địa chỉ IP thực tế
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_CurrCode", "VND");

            // Sắp xếp các tham số theo thứ tự alphabet
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Tạo chuỗi hash data và query string
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                    if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }

            // Tạo chữ ký
            String vnpSecureHash = hmacSHA512(hashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnpSecureHash);

            // Tạo URL đầy đủ
            return paymentUrl + "?" + query.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error creating VNPay payment URL", e);
        }
    }

    /**
     * Xác thực phản hồi từ VNPay
     */
    public boolean validatePaymentResponse(Map<String, String> vnpResponse) {
        try {
            String secureHash = vnpResponse.get("vnp_SecureHash");
            if (secureHash == null) {
                return false;
            }

            // Tạo dữ liệu để kiểm tra chữ ký
            Map<String, String> validParams = new HashMap<>(vnpResponse);
            validParams.remove("vnp_SecureHash");

            // Sắp xếp tham số theo thứ tự
            List<String> fieldNames = new ArrayList<>(validParams.keySet());
            Collections.sort(fieldNames);

            // Tạo chuỗi hash data
            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = validParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                    if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                        hashData.append('&');
                    }
                }
            }

            // Tính toán và so sánh chữ ký
            String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
            return calculatedHash.equalsIgnoreCase(secureHash);
        } catch (Exception e) {
            throw new RuntimeException("Error validating VNPay response", e);
        }
    }

    /**
     * Tạo HmacSHA512 từ key và data
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] result = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error creating HMAC SHA-512", e);
        }
    }

    /**
     * Chuyển mảng bytes thành chuỗi hex
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}