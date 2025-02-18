package com.example.api_sell_clothes_v1.DTO.Auth;

import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserProfileDTO {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Set<String> roles;
    private Set<String> permissions;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
}