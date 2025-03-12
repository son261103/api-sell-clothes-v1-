package com.example.api_sell_clothes_v1.Constants;

public class SecurityUrlConstants {
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/products/public/**",
            // Cart session URLs (for unauthenticated users)
            "/api/v1/carts/session/**",
            "/api/v1/cart-items/session/**",

            // Payment public URLs (for VNPay callback)
            "/api/v1/payment/confirm"  // Thêm mới: Callback từ VNPay cần public
    };

    public static final String[] ADMIN_URLS = {
            "/api/v1/admin/**",
    };

    public static final String[] USER_URLS = {
            "/api/v1/user/**",
    };
}