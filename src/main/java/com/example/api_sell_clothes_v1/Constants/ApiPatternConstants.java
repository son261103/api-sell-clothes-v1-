// ApiPatternConstants.java
package com.example.api_sell_clothes_v1.Constants;

public class ApiPatternConstants {
    // Public Api URLs
    public static final String API_PUBLIC = "/api/v1/public";

    // Base paths
    public static final String API_PERMISSIONS = "/api/v1/permissions";
    public static final String API_ROLES = "/api/v1/roles";
    public static final String API_BRANDS = "/api/v1/brands";
    public static final String API_PRODUCTS = "/api/v1/products";
    public static final String API_PRODUCT_VARIANTS = "/api/v1/product-variants";
    public static final String API_ORDERS = "/api/v1/orders";
    public static final String API_ORDER_ITEMS = "/api/v1/order-items";
    public static final String API_USERS = "/api/v1/users";
    public static final String API_USER_ADDRESSES = "/api/v1/user-addresses";
    public static final String API_CATEGORIES = "/api/v1/categories";
    public static final String API_REVIEWS = "/api/v1/reviews";
    public static final String API_CARTS = "/api/v1/carts";
    public static final String API_CART_ITEMS = "/api/v1/cart-items";
    // Payment-related base paths
    public static final String API_PAYMENT_METHODS = "/api/v1/payment-methods";
    public static final String API_PAYMENT = "/api/v1/payment";
    public static final String API_PAYMENT_HISTORY = "/api/v1/payment-history";

    // Common action patterns
    public static final String PATTERN_CREATE = "/create";
    public static final String PATTERN_EDIT = "/edit/**";
    public static final String PATTERN_DELETE = "/delete/**";
    public static final String PATTERN_VIEW = "/view/**";
    public static final String PATTERN_LIST = "/list";
}