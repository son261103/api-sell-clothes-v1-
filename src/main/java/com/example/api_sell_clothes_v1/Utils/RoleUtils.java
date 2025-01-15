package com.example.api_sell_clothes_v1.Utils;


import com.example.api_sell_clothes_v1.Entity.Roles;
import com.example.api_sell_clothes_v1.Enums.Types.RoleType;

public class RoleUtils {
    public static Roles createAdminRole() {
        return Roles.builder()
                .roleId(1L)
                .name(RoleType.ROLE_ADMIN.getCode())
                .description(RoleType.ROLE_ADMIN.getDescription())
                .build();
    }

    public static Roles createCustomerRole() {
        return Roles.builder()
                .roleId(2L)
                .name(RoleType.ROLE_CUSTOMER.getCode())
                .description(RoleType.ROLE_CUSTOMER.getDescription())
                .build();
    }
}