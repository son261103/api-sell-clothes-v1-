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
        put(ApiPatternConstants.API_USERS + "/username/**",
                PermissionType.VIEW_CUSTOMER.getCodeName());
        put(ApiPatternConstants.API_USERS + "/email/**",
                PermissionType.VIEW_CUSTOMER.getCodeName());
    }};

    // Category endpoint permissions
    public static final Map<String, String> CATEGORY_ENDPOINTS = new HashMap<>() {{
        // Parent category endpoints
        put(ApiPatternConstants.API_CATEGORIES + "/parent/create",
                PermissionType.CREATE_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/edit/**",
                PermissionType.EDIT_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/delete/**",
                PermissionType.DELETE_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/view/**",
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/list",
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/hierarchy/**",
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/by-name/**",
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/parent/by-slug/**",
                PermissionType.VIEW_CATEGORY.getCodeName());

        // Sub-category endpoints
        put(ApiPatternConstants.API_CATEGORIES + "/sub/create/**",
                PermissionType.CREATE_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/sub/edit/**",
                PermissionType.EDIT_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/sub/delete/**",
                PermissionType.DELETE_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/sub/view/**",
                PermissionType.VIEW_CATEGORY.getCodeName());
        put(ApiPatternConstants.API_CATEGORIES + "/sub/list/**",
                PermissionType.VIEW_CATEGORY.getCodeName());
    }};

    //    Brand endpoints permissions
    public static final Map<String, String> BRAND_ENDPOINTS = new HashMap<>() {{
        put(ApiPatternConstants.API_BRANDS + ApiPatternConstants.PATTERN_CREATE,
                PermissionType.CREATE_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + ApiPatternConstants.PATTERN_EDIT,
                PermissionType.EDIT_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + ApiPatternConstants.PATTERN_DELETE,
                PermissionType.DELETE_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + ApiPatternConstants.PATTERN_VIEW,
                PermissionType.VIEW_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + ApiPatternConstants.PATTERN_LIST,
                PermissionType.VIEW_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + "/active",
                PermissionType.VIEW_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + "/search",
                PermissionType.VIEW_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + "/status/{id}",
                PermissionType.EDIT_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + "/hierarchy",
                PermissionType.VIEW_BRAND.getCodeName());
        put(ApiPatternConstants.API_BRANDS + "/by-name/**",
                PermissionType.VIEW_BRAND.getCodeName());
    }};

    // Product endpoint permissions
    public static final Map<String, String> PRODUCT_ENDPOINTS = new HashMap<>() {{
        // Basic CRUD operations
        put(ApiPatternConstants.API_PRODUCTS + "/create",
                PermissionType.CREATE_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/edit/{id}",
                PermissionType.EDIT_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/delete/{id}",
                PermissionType.DELETE_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/view/{id}",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Viewing and listing endpoints
        put(ApiPatternConstants.API_PRODUCTS + "/hierarchy",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/slug/{slug}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/category/{categoryId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/brand/{brandId}",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Filtering and searching endpoints
        put(ApiPatternConstants.API_PRODUCTS + "/filter",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/search",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Status management
        put(ApiPatternConstants.API_PRODUCTS + "/status/{id}",
                PermissionType.EDIT_PRODUCT.getCodeName());

        // Special product listings
        put(ApiPatternConstants.API_PRODUCTS + "/sale",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/latest",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/featured",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/related/{productId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/bulk-create",
                PermissionType.CREATE_PRODUCT.getCodeName());
    }};

    // Product variants endpoint permission
    public static final Map<String, String> PRODUCT_VARIANT_ENDPOINTS = new HashMap<>() {{
        // Basic CRUD operations
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/create",
                PermissionType.CREATE_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/edit/{id}",
                PermissionType.EDIT_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/delete/{id}",
                PermissionType.DELETE_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/view/{id}",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Viewing and listing endpoints
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/hierarchy/{productId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/sku/{sku}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/product/{productId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/product/{productId}/active",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Filtering endpoints
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/filter",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Product-specific attributes
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/product/{productId}/sizes",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/product/{productId}/colors",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Availability and stock management
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/check-availability",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/low-stock",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/out-of-stock",
                PermissionType.VIEW_PRODUCT.getCodeName());

        // Status and stock management
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/status/{id}",
                PermissionType.EDIT_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCT_VARIANTS + "/{id}/stock",
                PermissionType.EDIT_PRODUCT.getCodeName());
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