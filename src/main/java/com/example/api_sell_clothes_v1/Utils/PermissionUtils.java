package com.example.api_sell_clothes_v1.Utils;

import com.example.api_sell_clothes_v1.Entity.Permissions;
import com.example.api_sell_clothes_v1.Enums.Types.PermissionType;

public class PermissionUtils {

    // Tạo quyền tạo khách hàng
    public static Permissions createCustomerPermission() {
        return Permissions.builder()
                .permissionId(9L)
                .name(PermissionType.CREATE_CUSTOMER.getName())
                .codeName(PermissionType.CREATE_CUSTOMER.getCodeName())
                .description(PermissionType.CREATE_CUSTOMER.getDescription())
                .groupName(PermissionType.CREATE_CUSTOMER.getGroupName())
                .build();
    }


    // Tạo quyền sửa khách hàng
    public static Permissions updateCustomerPermission() {
        return Permissions.builder()
                .permissionId(9L)
                .name(PermissionType.EDIT_CUSTOMER.getName())
                .codeName(PermissionType.EDIT_CUSTOMER.getCodeName())
                .description(PermissionType.EDIT_CUSTOMER.getDescription())
                .groupName(PermissionType.EDIT_CUSTOMER.getGroupName())
                .build();
    }

    // Tạo quyền xóa khách hàng
    public static Permissions deleteCustomerPermission() {
        return Permissions.builder()
                .permissionId(10L)
                .name(PermissionType.DELETE_CUSTOMER.getName())
                .codeName(PermissionType.DELETE_CUSTOMER.getCodeName())
                .description(PermissionType.DELETE_CUSTOMER.getDescription())
                .groupName(PermissionType.DELETE_CUSTOMER.getGroupName())
                .build();
    }

    // Tạo quyền xem khách hàng
    public static Permissions viewCustomerPermission() {
        return Permissions.builder()
                .permissionId(11L)
                .name(PermissionType.VIEW_CUSTOMER.getName())
                .codeName(PermissionType.VIEW_CUSTOMER.getCodeName())
                .description(PermissionType.VIEW_CUSTOMER.getDescription())
                .groupName(PermissionType.VIEW_CUSTOMER.getGroupName())
                .build();
    }



    // Tạo quyền sửa danh mục
    public static Permissions createCategoryPermission() {
        return Permissions.builder()
                .permissionId(9L)
                .name(PermissionType.CREATE_CATEGORY.getName())
                .codeName(PermissionType.CREATE_CATEGORY.getCodeName())
                .description(PermissionType.CREATE_CATEGORY.getDescription())
                .groupName(PermissionType.CREATE_CATEGORY.getGroupName())
                .build();
    }


    // Tạo quyền sửa danh mục
    public static Permissions updateCategoryPermission() {
        return Permissions.builder()
                .permissionId(9L)
                .name(PermissionType.EDIT_CATEGORY.getName())
                .codeName(PermissionType.EDIT_CATEGORY.getCodeName())
                .description(PermissionType.EDIT_CATEGORY.getDescription())
                .groupName(PermissionType.EDIT_CATEGORY.getGroupName())
                .build();
    }

    // Tạo quyền xóa danh mục
    public static Permissions deleteCategoryPermission() {
        return Permissions.builder()
                .permissionId(10L)
                .name(PermissionType.DELETE_CATEGORY.getName())
                .codeName(PermissionType.DELETE_CATEGORY.getCodeName())
                .description(PermissionType.DELETE_CATEGORY.getDescription())
                .groupName(PermissionType.DELETE_CATEGORY.getGroupName())
                .build();
    }

    // Tạo quyền xem danh mục
    public static Permissions viewCategoryPermission() {
        return Permissions.builder()
                .permissionId(11L)
                .name(PermissionType.VIEW_CATEGORY.getName())
                .codeName(PermissionType.VIEW_CATEGORY.getCodeName())
                .description(PermissionType.VIEW_CATEGORY.getDescription())
                .groupName(PermissionType.VIEW_CATEGORY.getGroupName())
                .build();
    }
}
