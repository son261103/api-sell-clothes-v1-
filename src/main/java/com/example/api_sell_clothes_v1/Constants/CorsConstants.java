// CorsConstants.java
package com.example.api_sell_clothes_v1.Constants;

public class CorsConstants {
    public static final String[] ALLOWED_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:5173",
            "http://localhost:3001",
            "http://localhost:5174"
    };

    public static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    };

    public static final String[] ALLOWED_HEADERS = {
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-User-Id"
    };

    public static final long MAX_AGE = 3600L;
}