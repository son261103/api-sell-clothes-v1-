package com.example.api_sell_clothes_v1.DTO.UserAddress;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequestDTO {
    @NotBlank(message = "Địa chỉ không được để trống")
    private String addressLine;

    private String city;

    private String district;

    private String ward;

    private Boolean isDefault;
}
