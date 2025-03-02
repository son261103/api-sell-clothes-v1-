package com.example.api_sell_clothes_v1.DTO.Auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChangePasswordWithOtpDTO {
    @NotBlank(message = "Email/Phone is required")
    private String loginId; // Email hoặc số điện thoại

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "OTP is required")
    private String otp;
}
