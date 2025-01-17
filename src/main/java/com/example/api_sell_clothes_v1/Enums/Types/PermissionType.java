package com.example.api_sell_clothes_v1.Enums.Types;

import lombok.Getter;

@Getter
public enum PermissionType {


    //    Role Management Permissions
    CREATE_ROLE("CREATE_ROLE", "Create Role", "Allows ADMIN to create new roles", "Role Management"),
    EDIT_ROLE("EDIT_ROLE", "Edit Role", "Allows ADMIN to edit existing roles", "Role Management"),
    DELETE_ROLE("DELETE_ROLE", "Delete Role", "Allows ADMIN to delete roles", "Role Management"),
    VIEW_ROLE("VIEW_ROLE", "View Role", "Allows ADMIN to view role details", "Role Management"),

    //    Permission Management Permissions
    CREATE_PERMISSION("CREATE_PERMISSION", "Create Permission", "Allows ADMIN to create new permissions", "Permission Management"),
    EDIT_PERMISSION("EDIT_PERMISSION", "Edit Permission", "Allows ADMIN to edit existing permissions", "Permission Management"),
    DELETE_PERMISSION("DELETE_PERMISSION", "Delete Permission", "Allows ADMIN to delete permissions", "Permission Management"),
    VIEW_PERMISSION("VIEW_PERMISSION", "View Permission", "Allows ADMIN to view permission details", "Permission Management"),

    // CUSTOMER Management Permissions
    CREATE_CUSTOMER("CREATE_CUSTOMER", "Create CUSTOMER", "Allows CUSTOMER to create new CUSTOMER accounts", "CUSTOMER Management"),
    EDIT_CUSTOMER("EDIT_CUSTOMER", "Edit CUSTOMER", "Allows CUSTOMER to edit CUSTOMER account details", "CUSTOMER Management"),
    DELETE_CUSTOMER("DELETE_CUSTOMER", "Delete CUSTOMER", "Allows CUSTOMER to delete CUSTOMER accounts", "CUSTOMER Management"),
    VIEW_CUSTOMER("VIEW_CUSTOMER", "View CUSTOMER", "Allows CUSTOMER to view CUSTOMER account details", "CUSTOMER Management"),

    // Category Management Permissions
    CREATE_CATEGORY("CREATE_CATEGORY", "Create Category", "Allows CUSTOMER to create a new product category", "Category Management"),
    EDIT_CATEGORY("EDIT_CATEGORY", "Edit Category", "Allows CUSTOMER to edit an existing product category", "Category Management"),
    DELETE_CATEGORY("DELETE_CATEGORY", "Delete Category", "Allows CUSTOMER to delete a product category", "Category Management"),
    VIEW_CATEGORY("VIEW_CATEGORY", "View Category", "Allows CUSTOMER to view category details", "Category Management"),

    //    Brand Management Permissions
    CREATE_BRAND("CREATE_BRAND", "Create Brand", "Allows CUSTOMER to create a new product brand", "Brand Management"),
    EDIT_BRAND("EDIT_BRAND", "Edit Brand", "Allows CUSTOMER to edit an existing product brand", "Brand Management"),
    DELETE_BRAND("DELETE_BRAND", "Delete Brand", "Allows CUSTOMER to delete a product brand", "Brand Management"),
    VIEW_BRAND("VIEW_BRAND", "View Brand", "Allows CUSTOMER to view brand details", "Brand Management"),

    // Product Management Permissions
    ADD_PRODUCT("ADD_PRODUCT", "Add Product", "Allows CUSTOMER to add a new product", "Product Management"),
    EDIT_PRODUCT("EDIT_PRODUCT", "Edit Product", "Allows CUSTOMER to edit an existing product", "Product Management"),
    DELETE_PRODUCT("DELETE_PRODUCT", "Delete Product", "Allows CUSTOMER to delete a product", "Product Management"),
    VIEW_PRODUCT("VIEW_PRODUCT", "View Product", "Allows CUSTOMER to view product details", "Product Management"),

    // Order Management Permissions
    CREATE_ORDER("CREATE_ORDER", "Create Order", "Allows CUSTOMER to create new orders", "Order Management"),
    EDIT_ORDER("EDIT_ORDER", "Edit Order", "Allows CUSTOMER to edit an existing order", "Order Management"),
    DELETE_ORDER("DELETE_ORDER", "Delete Order", "Allows CUSTOMER to delete an order", "Order Management"),
    VIEW_ORDER("VIEW_ORDER", "View Order", "Allows CUSTOMER to view order details", "Order Management"),


    // Review Management Permissions
    CREATE_REVIEW("CREATE_REVIEW", "Create Review", "Allows CUSTOMER to create a review for a product", "Review Management"),
    EDIT_REVIEW("EDIT_REVIEW", "Edit Review", "Allows CUSTOMER to edit a review", "Review Management"),
    DELETE_REVIEW("DELETE_REVIEW", "Delete Review", "Allows CUSTOMER to delete a review", "Review Management"),
    VIEW_REVIEW("VIEW_REVIEW", "View Review", "Allows CUSTOMER to view product reviews", "Review Management");

    private final String codeName;
    private final String name;
    private final String description;
    private final String groupName;

    // Constructor
    PermissionType(String codeName, String name, String description, String groupName) {
        this.codeName = codeName;
        this.name = name;
        this.description = description;
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return "Permission{codeName='" + codeName + "', name='" + name + "', description='" + description + "', groupName='" + groupName + "'}";
    }
}
