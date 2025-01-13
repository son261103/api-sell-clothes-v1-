package com.example.api_sell_clothes_v1.DTO.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RegisterResponseDTO {
    private Long userId;
    private String username;
    private String email;
    private String message;
    private boolean requiresEmailVerification;
}