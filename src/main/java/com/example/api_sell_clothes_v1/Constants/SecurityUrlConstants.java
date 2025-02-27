package com.example.api_sell_clothes_v1.Constants;

public class SecurityUrlConstants {
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/api/v1/public/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/products/public/**",
            "/api/v1/public/**",
            // Cart session URLs (for unauthenticated users)
            "/api/v1/carts/session/**",
            "/api/v1/cart-items/session/**",

            // Payment public URLs (for VNPay callback)
            "/api/v1/payment/confirm"  // Thêm mới: Callback từ VNPay cần public
    };

    public static final String[] ADMIN_URLS = {
            "/api/v1/admin/**",
            "/api/v1/orders/admin/**",          // Admin order endpoints
            "/api/v1/order-items/admin/**",     // Admin order-item endpoints
            "/api/v1/user/addresses/admin/**",  // Admin user address endpoints
            "/api/v1/payment-methods/admin/**", // Admin payment method endpoints (thêm mới)
            "/api/v1/payment/admin/**",         // Admin payment endpoints (thêm mới)
            "/api/v1/payment-history/admin/**"  // Admin payment history endpoints (thêm mới)
    };

    public static final String[] USER_URLS = {
            "/api/v1/user/**",
            "/api/v1/orders/**",                // User order endpoints (non-admin)
            "/api/v1/order-items/**",           // User order-item endpoints (non-admin)
            "/api/v1/carts/**",                 // User cart endpoints (non-session)
            "/api/v1/cart-items/**",            // User cart-item endpoints (non-session)
            "/api/v1/user/addresses/**",        // User address endpoints (non-admin)
            "/api/v1/payment-methods/**",       // User payment method endpoints (non-admin, thêm mới)
            "/api/v1/payment/**",               // User payment endpoints (non-admin, thêm mới)
            "/api/v1/payment-history/**"        // User payment history endpoints (non-admin, thêm mới)
    };
}