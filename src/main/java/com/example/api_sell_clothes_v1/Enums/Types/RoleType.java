package com.example.api_sell_clothes_v1.Enums.Types;

import lombok.Getter;

@Getter
public enum RoleType {

    // Các giá trị role được gộp trực tiếp trong enum này
    ROLE_ADMIN("ROLE_ADMIN", "Admin", "Administrator role with full permissions"),
    ROLE_CUSTOMER("ROLE_CUSTOMER", "Customer", "Customer role with limited permissions"),
    ROLE_MODERATOR("ROLE_MODERATOR", "Moderator", "Moderator role with permissions to manage content"),
    ROLE_GUEST("ROLE_GUEST", "Guest", "Guest role with read-only permissions");

    private final String code;        // Mã role
    private final String name;        // Tên role
    private final String description; // Mô tả role

    // Constructor
    RoleType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
