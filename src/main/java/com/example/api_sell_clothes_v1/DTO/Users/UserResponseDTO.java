package com.example.api_sell_clothes_v1.DTO.Users;


import com.example.api_sell_clothes_v1.DTO.Roles.RoleResponseDTO;
import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private String avatar;
    private String lastLoginAt;
    private String fullName;
    private UserStatus status;
    private Set<RoleResponseDTO> roles;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
}