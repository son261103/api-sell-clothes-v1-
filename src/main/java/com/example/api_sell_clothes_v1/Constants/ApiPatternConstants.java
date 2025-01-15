// ApiPatternConstants.java
package com.example.api_sell_clothes_v1.Constants;

public class ApiPatternConstants {
    // Base paths
    public static final String API_PERMISSIONS = "/api/v1/permissions";
    public static final String API_ROLES = "/api/v1/roles";
    public static final String API_PRODUCTS = "/api/v1/products";
    public static final String API_ORDERS = "/api/v1/orders";
    public static final String API_USERS = "/api/v1/users";
    public static final String API_CATEGORIES = "/api/v1/categories";
    public static final String API_REVIEWS = "/api/v1/reviews";

    // Common action patterns
    public static final String PATTERN_ADD = "/add";
    public static final String PATTERN_CREATE = "/create";
    public static final String PATTERN_EDIT = "/edit/**";
    public static final String PATTERN_DELETE = "/delete/**";
    public static final String PATTERN_VIEW = "/view/**";
    public static final String PATTERN_LIST = "/list";
    public static final String PATTERN_CANCEL = "/cancel/**";
}