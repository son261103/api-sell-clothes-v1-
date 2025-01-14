package com.example.api_sell_clothes_v1.Enums.Types;


import com.example.api_sell_clothes_v1.Constants.RoleConstants;
import lombok.Getter;

@Getter
public enum RoleType {
    ROLE_ADMIN(RoleConstants.ROLE_ADMIN, RoleConstants.ADMIN_NAME, RoleConstants.ADMIN_DESCRIPTION),
    ROLE_CUSTOMER(RoleConstants.ROLE_CUSTOMER, RoleConstants.CUSTOMER_NAME, RoleConstants.CUSTOMER_DESCRIPTION),
    ROLE_MODERATOR(RoleConstants.ROLE_MODERATOR, RoleConstants.MODERATOR_NAME, RoleConstants.MODERATOR_DESCRIPTION),
    ROLE_GUEST(RoleConstants.ROLE_GUEST, RoleConstants.GUEST_NAME, RoleConstants.GUEST_DESCRIPTION);

    private final String code;
    private final String name;
    private final String description;

    RoleType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

}
