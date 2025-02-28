package com.example.api_sell_clothes_v1.DTO.UserAddress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDTO {
    private Long addressId;
    private Long userId;
    private String addressLine;
    private String city;
    private String district;
    private String ward;
    private String phoneNumber;
    private Boolean isDefault;

    // Full address for display purposes
    private String fullAddress;
}