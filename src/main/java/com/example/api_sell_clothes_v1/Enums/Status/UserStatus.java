package com.example.api_sell_clothes_v1.Enums.Status;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE(1, "Active"),
    LOCKED(2, "Locked"),
    BANNER(3, "Banner"),
    PENDING(4, "Pending");

    private final int code;
    private final String description;

    UserStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus status : UserStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid UserStatus code: " + code);
    }
}
