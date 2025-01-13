package com.example.api_sell_clothes_v1.Utils;

import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Enums.Types.PermissionType;

public class PermissionUtils {

    // Tạo quyền quản lý người dùng
    public static Permissions createManageUsersPermission() {
        return Permissions.builder()
                .permissionId(1L)
                .name(PermissionType.MANAGE_USERS.getName())
                .codeName(PermissionType.MANAGE_USERS.getCode())
                .description(PermissionType.MANAGE_USERS.getDescription())
                .groupName(PermissionType.MANAGE_USERS.getGroupName())
                .build();
    }

    // Tạo quyền quản lý sản phẩm
    public static Permissions createManageProductsPermission() {
        return Permissions.builder()
                .permissionId(2L)
                .name(PermissionType.MANAGE_PRODUCTS.getName())
                .codeName(PermissionType.MANAGE_PRODUCTS.getCode())
                .description(PermissionType.MANAGE_PRODUCTS.getDescription())
                .groupName(PermissionType.MANAGE_PRODUCTS.getGroupName())
                .build();
    }

    // Tạo quyền quản lý đơn hàng
    public static Permissions createManageOrdersPermission() {
        return Permissions.builder()
                .permissionId(3L)
                .name(PermissionType.MANAGE_ORDERS.getName())
                .codeName(PermissionType.MANAGE_ORDERS.getCode())
                .description(PermissionType.MANAGE_ORDERS.getDescription())
                .groupName(PermissionType.MANAGE_ORDERS.getGroupName())
                .build();
    }

    // Tạo quyền quản lý vai trò
    public static Permissions createManageRolesPermission() {
        return Permissions.builder()
                .permissionId(4L)
                .name(PermissionType.MANAGE_ROLES.getName())
                .codeName(PermissionType.MANAGE_ROLES.getCode())
                .description(PermissionType.MANAGE_ROLES.getDescription())
                .groupName(PermissionType.MANAGE_ROLES.getGroupName())
                .build();
    }
}
