package com.example.api_sell_clothes_v1.Constants;

import java.util.HashMap;
import java.util.Map;

public class EndpointPermissionConstants {

    // Product endpoint permissions
    public static final Map<String, String> PRODUCT_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_ADD,
                PermissionConstants.ADD_PRODUCT);
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_EDIT,
                PermissionConstants.EDIT_PRODUCT);
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_DELETE,
                PermissionConstants.DELETE_PRODUCT);
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_VIEW,
                PermissionConstants.VIEW_PRODUCT);
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_LIST,
                PermissionConstants.VIEW_PRODUCT);
    }};

    // Order endpoint permissions
    public static final Map<String, String> ORDER_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_CREATE,
                PermissionConstants.CREATE_ORDER);
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_EDIT,
                PermissionConstants.EDIT_ORDER);
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_DELETE,
                PermissionConstants.DELETE_ORDER);
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_VIEW,
                PermissionConstants.VIEW_ORDER);
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_LIST,
                PermissionConstants.VIEW_ORDER);
    }};

    // User endpoint permissions
    public static final Map<String, String> USER_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_CREATE,
                PermissionConstants.CREATE_USER);
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_EDIT,
                PermissionConstants.EDIT_USER);
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_DELETE,
                PermissionConstants.DELETE_USER);
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_VIEW,
                PermissionConstants.VIEW_USER);
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_LIST,
                PermissionConstants.VIEW_USER);
    }};

    // Category endpoint permissions
    public static final Map<String, String> CATEGORY_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_CREATE,
                PermissionConstants.CREATE_CATEGORY);
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_EDIT,
                PermissionConstants.EDIT_CATEGORY);
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_DELETE,
                PermissionConstants.DELETE_CATEGORY);
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_VIEW,
                PermissionConstants.VIEW_CATEGORY);
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_LIST,
                PermissionConstants.VIEW_CATEGORY);
        put(ApiPatternConstants.API_CATEGORIES + "/sub-categories/**",
                PermissionConstants.VIEW_CATEGORY);
        put(ApiPatternConstants.API_CATEGORIES + "/parent-categories",
                PermissionConstants.VIEW_CATEGORY);
    }};

    // Review endpoint permissions
    public static final Map<String, String> REVIEW_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_CREATE,
                PermissionConstants.CREATE_REVIEW);
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_EDIT,
                PermissionConstants.EDIT_REVIEW);
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_DELETE,
                PermissionConstants.DELETE_REVIEW);
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_VIEW,
                PermissionConstants.VIEW_REVIEW);
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_LIST,
                PermissionConstants.VIEW_REVIEW);
    }};
}