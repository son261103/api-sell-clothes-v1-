package com.example.api_sell_clothes_v1.DTO.UserAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressDTO {
    @NotBlank(message = "Địa chỉ không được để trống")
    private String addressLine;

    private String city;

    private String district;

    private String ward;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ (phải có 10-11 số)")
    private String phoneNumber;

    private Boolean isDefault;
}