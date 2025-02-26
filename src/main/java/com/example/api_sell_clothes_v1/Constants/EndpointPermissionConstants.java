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

    public static final Map<String, String> PRODUCT_IMAGE_ENDPOINTS = new HashMap<>() {{
        // Upload and update operations
        put(ApiPatternConstants.API_PRODUCTS + "/images/upload/{productId}",
                PermissionType.EDIT_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/images/update-file/{imageId}",
                PermissionType.EDIT_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/images/update/{imageId}",
                PermissionType.EDIT_PRODUCT.getCodeName());

        // Primary image management
        put(ApiPatternConstants.API_PRODUCTS + "/images/primary/{imageId}",
                PermissionType.EDIT_PRODUCT.getCodeName());

        // Image organization
        put(ApiPatternConstants.API_PRODUCTS + "/images/reorder/{productId}",
                PermissionType.EDIT_PRODUCT.getCodeName());

        // Delete operations
        put(ApiPatternConstants.API_PRODUCTS + "/images/{imageId}",
                PermissionType.DELETE_PRODUCT.getCodeName());

        // View operations
        put(ApiPatternConstants.API_PRODUCTS + "/images/list/{productId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/images/primary/{productId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
        put(ApiPatternConstants.API_PRODUCTS + "/images/hierarchy/{productId}",
                PermissionType.VIEW_PRODUCT.getCodeName());
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
        // User order endpoints
        put(ApiPatternConstants.API_ORDERS + "/create",
                PermissionType.CHECKOUT_CART.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/{orderId}",
                PermissionType.VIEW_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS,
                PermissionType.VIEW_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/status/{status}",
                PermissionType.VIEW_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/{orderId}/cancel",
                PermissionType.CANCEL_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/bestselling",
                PermissionType.VIEW_ORDER.getCodeName());

        // Admin order endpoints
        put(ApiPatternConstants.API_ORDERS + "/admin/{orderId}",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/list",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/status/{status}",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/search",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/filter",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/{orderId}/status",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/statistics",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDERS + "/admin/{orderId}",
                PermissionType.MANAGE_ORDER.getCodeName());
    }};


    // OrderItem endpoint permissions
    public static final Map<String, String> ORDER_ITEM_ENDPOINTS = new HashMap<>() {{
        // User orderitem endpoints
        put(ApiPatternConstants.API_ORDER_ITEMS + "/order/{orderId}",
                PermissionType.VIEW_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDER_ITEMS + "/{orderItemId}",
                PermissionType.VIEW_ORDER.getCodeName());

        // Admin orderitem endpoints
        put(ApiPatternConstants.API_ORDER_ITEMS + "/admin/order/{orderId}",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDER_ITEMS + "/admin/bestselling-variants",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDER_ITEMS + "/admin/bestselling-products",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDER_ITEMS + "/admin/product-sales/{productId}",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDER_ITEMS + "/admin/restore-inventory/{orderId}",
                PermissionType.MANAGE_ORDER.getCodeName());
        put(ApiPatternConstants.API_ORDER_ITEMS + "/admin/order/{orderId}",
                PermissionType.MANAGE_ORDER.getCodeName());
    }};

    public static final Map<String, String> USER_ADDRESS_ENDPOINTS = new HashMap<>() {{
        // User address endpoints
        put(ApiPatternConstants.API_USER_ADDRESSES,
                PermissionType.VIEW_ADDRESS.getCodeName()); // GET: List all addresses
        put(ApiPatternConstants.API_USER_ADDRESSES + "/{addressId}",
                PermissionType.VIEW_ADDRESS.getCodeName()); // GET: View specific address
        put(ApiPatternConstants.API_USER_ADDRESSES,
                PermissionType.CREATE_ADDRESS.getCodeName()); // POST: Create new address
        put(ApiPatternConstants.API_USER_ADDRESSES + "/{addressId}",
                PermissionType.EDIT_ADDRESS.getCodeName()); // PUT: Update address
        put(ApiPatternConstants.API_USER_ADDRESSES + "/{addressId}",
                PermissionType.DELETE_ADDRESS.getCodeName()); // DELETE: Remove address
        put(ApiPatternConstants.API_USER_ADDRESSES + "/{addressId}/default",
                PermissionType.SET_DEFAULT_ADDRESS.getCodeName()); // Set as default
        put(ApiPatternConstants.API_USER_ADDRESSES + "/default",
                PermissionType.VIEW_ADDRESS.getCodeName()); // GET: View default address
        put(ApiPatternConstants.API_USER_ADDRESSES + "/count",
                PermissionType.VIEW_ADDRESS.getCodeName()); // GET: Count addresses
        put(ApiPatternConstants.API_USER_ADDRESSES + "/check/{addressId}",
                PermissionType.VIEW_ADDRESS.getCodeName()); // GET: Check address
        put(ApiPatternConstants.API_USER_ADDRESSES + "/check/{addressId}/owner",
                PermissionType.VIEW_ADDRESS.getCodeName()); // GET: Check address ownership
        put(ApiPatternConstants.API_USER_ADDRESSES + "/validate",
                PermissionType.VALIDATE_ADDRESS.getCodeName()); // Validate address
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

    // Cart endpoint permissions
    public static final Map<String, String> CART_ENDPOINTS = new HashMap<>() {{
        // User cart endpoints
        put(ApiPatternConstants.API_CARTS,
                PermissionType.VIEW_CART.getCodeName());
        put(ApiPatternConstants.API_CARTS + "/summary",
                PermissionType.VIEW_CART.getCodeName());
        put(ApiPatternConstants.API_CARTS + "/clear",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CARTS + "/selected",
                PermissionType.VIEW_CART.getCodeName());
        put(ApiPatternConstants.API_CARTS + "/count",
                PermissionType.VIEW_CART.getCodeName());

        // Session cart endpoints (không cần phân quyền)
        put(ApiPatternConstants.API_CARTS + "/session/{sessionId}",
                null);
        put(ApiPatternConstants.API_CARTS + "/session/{sessionId}/summary",
                null);
        put(ApiPatternConstants.API_CARTS + "/session/{sessionId}/clear",
                null);
        put(ApiPatternConstants.API_CARTS + "/session/{sessionId}/selected",
                null);
        put(ApiPatternConstants.API_CARTS + "/session/{sessionId}/count",
                null);
        put(ApiPatternConstants.API_CARTS + "/session/{sessionId}/merge",
                PermissionType.CHECKOUT_CART.getCodeName());
    }};

    // Cart Item endpoint permissions
    public static final Map<String, String> CART_ITEM_ENDPOINTS = new HashMap<>() {{
        // User cart item endpoints
        put(ApiPatternConstants.API_CART_ITEMS + "/add",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/{cartItemId}/quantity",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/{cartItemId}/select",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/{cartItemId}",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/select-all",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/deselect-all",
                PermissionType.EDIT_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS,
                PermissionType.VIEW_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/check",
                PermissionType.VIEW_CART.getCodeName());
        put(ApiPatternConstants.API_CART_ITEMS + "/count-selected",
                PermissionType.VIEW_CART.getCodeName());

        // Session cart item endpoints (không cần phân quyền)
        put(ApiPatternConstants.API_CART_ITEMS + "/session/{sessionId}/add",
                null);
        put(ApiPatternConstants.API_CART_ITEMS + "/session/{sessionId}",
                null);
        put(ApiPatternConstants.API_CART_ITEMS + "/session/{sessionId}/check",
                null);
        put(ApiPatternConstants.API_CART_ITEMS + "/session/{sessionId}/count-selected",
                null);
        put(ApiPatternConstants.API_CART_ITEMS + "/session/{sessionId}/select-all",
                null);
        put(ApiPatternConstants.API_CART_ITEMS + "/session/{sessionId}/deselect-all",
                null);
    }};


    // Payment Method endpoint permissions
    public static final Map<String, String> PAYMENT_METHOD_ENDPOINTS = new HashMap<>() {{
        // User payment method endpoints
        put(ApiPatternConstants.API_PAYMENT_METHODS,
                PermissionType.VIEW_PAYMENT_METHOD.getCodeName()); // GET: List all payment methods

        // Admin payment method endpoints
        put(ApiPatternConstants.API_PAYMENT_METHODS + "/admin/list",
                PermissionType.MANAGE_PAYMENT_METHOD.getCodeName()); // GET: Paginated list
        put(ApiPatternConstants.API_PAYMENT_METHODS + "/admin/{methodId}",
                PermissionType.MANAGE_PAYMENT_METHOD.getCodeName()); // GET: View by ID
        put(ApiPatternConstants.API_PAYMENT_METHODS + "/admin/create",
                PermissionType.MANAGE_PAYMENT_METHOD.getCodeName()); // POST: Create
        put(ApiPatternConstants.API_PAYMENT_METHODS + "/admin/{methodId}",
                PermissionType.MANAGE_PAYMENT_METHOD.getCodeName()); // PUT: Update
        put(ApiPatternConstants.API_PAYMENT_METHODS + "/admin/{methodId}",
                PermissionType.MANAGE_PAYMENT_METHOD.getCodeName()); // DELETE: Delete
    }};

    // Payment endpoint permissions
    public static final Map<String, String> PAYMENT_ENDPOINTS = new HashMap<>() {{
        // User payment endpoints
        put(ApiPatternConstants.API_PAYMENT + "/create",
                PermissionType.CREATE_PAYMENT.getCodeName()); // POST: Create payment
        put(ApiPatternConstants.API_PAYMENT + "/order/{orderId}",
                PermissionType.VIEW_PAYMENT.getCodeName()); // GET: View payment by order ID

        // Public endpoint (VNPay callback, không cần quyền)
        put(ApiPatternConstants.API_PAYMENT + "/confirm",
                null); // GET: Confirm VNPay payment

        // Admin payment endpoints
        put(ApiPatternConstants.API_PAYMENT + "/admin/order/{orderId}",
                PermissionType.MANAGE_PAYMENT.getCodeName()); // GET: View payment by order ID
    }};

    // Payment History endpoint permissions
    public static final Map<String, String> PAYMENT_HISTORY_ENDPOINTS = new HashMap<>() {{
        // User payment history endpoints
        put(ApiPatternConstants.API_PAYMENT_HISTORY + "/payment/{paymentId}",
                PermissionType.VIEW_PAYMENT_HISTORY.getCodeName()); // GET: History by payment ID
        put(ApiPatternConstants.API_PAYMENT_HISTORY + "/order/{orderId}",
                PermissionType.VIEW_PAYMENT_HISTORY.getCodeName()); // GET: History by order ID

        // Admin payment history endpoints
        put(ApiPatternConstants.API_PAYMENT_HISTORY + "/admin/payment/{paymentId}",
                PermissionType.MANAGE_PAYMENT_HISTORY.getCodeName()); // GET: History by payment ID
        put(ApiPatternConstants.API_PAYMENT_HISTORY + "/admin/order/{orderId}",
                PermissionType.MANAGE_PAYMENT_HISTORY.getCodeName()); // GET: History by order ID
        put(ApiPatternConstants.API_PAYMENT_HISTORY + "/admin/list",
                PermissionType.MANAGE_PAYMENT_HISTORY.getCodeName()); // GET: Paginated list
    }};



}