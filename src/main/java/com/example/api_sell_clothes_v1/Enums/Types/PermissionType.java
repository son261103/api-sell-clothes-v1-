package com.example.api_sell_clothes_v1.Enums.Types;

import com.example.api_sell_clothes_v1.Enums.Constants.PermissionConstants;
import lombok.Getter;

@Getter
public enum PermissionType {
    MANAGE_USERS("manage_users", "Quản lý người dùng", PermissionConstants.USER_MANAGEMENT_GROUP, PermissionConstants.MANAGE_USERS_DESC),
    MANAGE_PRODUCTS("manage_products", "Quản lý sản phẩm", PermissionConstants.PRODUCT_MANAGEMENT_GROUP, PermissionConstants.MANAGE_PRODUCTS_DESC),
    MANAGE_ORDERS("manage_orders", "Quản lý đơn hàng", PermissionConstants.ORDER_MANAGEMENT_GROUP, PermissionConstants.MANAGE_ORDERS_DESC),
    MANAGE_ROLES("manage_roles", "Quản lý vai trò", PermissionConstants.USER_MANAGEMENT_GROUP, PermissionConstants.MANAGE_ROLES_DESC);

    private final String code;
    private final String name;
    private final String groupName;
    private final String description;

    PermissionType(String code, String name, String groupName, String description) {
        this.code = code;
        this.name = name;
        this.groupName = groupName;
        this.description = description;
    }
}
