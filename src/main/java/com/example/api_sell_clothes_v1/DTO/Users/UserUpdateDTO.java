package com.example.api_sell_clothes_v1.DTO.Users;


import com.example.api_sell_clothes_v1.Enums.Status.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UserUpdateDTO {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String avatar;
    private UserStatus status;
}
