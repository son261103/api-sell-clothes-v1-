package com.example.api_sell_clothes_v1.Constants;

import com.example.api_sell_clothes_v1.Enums.Types.PermissionType;

import java.util.HashMap;
import java.util.Map;

public class EndpointPermissionConstants {

    // Add this to your EndpointPermissionConstants class
    public static final Map<String, String> PERMISSION_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_PERMISSIONS + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_PERMISSION.getCodeName());
        put(ApiPatternConstants.API_PERMISSIONS + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_PERMISSION.getCodeName());
        put(ApiPatternConstants.API_PERMISSIONS + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_PERMISSION.getCodeName());
        put(ApiPatternConstants.API_PERMISSIONS + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_PERMISSION.getCodeName());
        put(ApiPatternConstants.API_PERMISSIONS + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_PERMISSION.getCodeName());
        put(ApiPatternConstants.API_PERMISSIONS + "/group/**",
                PermissionType.VIEW_PERMISSION.getCodeName());
        put(ApiPatternConstants.API_PERMISSIONS + "/check-exists",
                PermissionType.VIEW_PERMISSION.getCodeName());
    }};

    // Add this to your EndpointRoleConstants class
    public static final Map<String, String> ROLE_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_ROLES + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + "/{roleId}/permissions/**",
                PermissionType.EDIT_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + "/check-exists",
                PermissionType.VIEW_ROLE.getCodeName());
        put(ApiPatternConstants.API_ROLES + "/name/**",
                PermissionType.VIEW_ROLE.getCodeName());
    }};


    // Product endpoint permissions
    public static final Map<String, String> PRODUCT_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_ADD,
                PermissionType.ADD_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_PRODUCT.getCodeName());
    }};

    // Order endpoint permissions
    public static final Map<String, String> ORDER_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_ORDER.getCodeName());
    }};

    // User endpoint permissions
    public static final Map<String, String> USER_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_CUSTOMER.getCodeName());
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_CUSTOMER.getCodeName());
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_CUSTOMER.getCodeName());
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_CUSTOMER.getCodeName());
        put(ApiPatternConstants.API_USERS + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_CUSTOMER.getCodeName());
    }};

    // Category endpoint permissions
    public static final Map<String, String> CATEGORY_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/sub-categories/**",
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent-categories",
                PermissionType.VIEW_CATEGORY.getCodeName());
    }};

    // Review endpoint permissions
    public static final Map<String, String> REVIEW_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_REVIEW.getCodeName());
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_REVIEW.getCodeName());
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_REVIEW.getCodeName());
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_REVIEW.getCodeName());
        put(ApiPatternConstants.API_REVIEWS + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_REVIEW.getCodeName());
    }};
}