package com.example.api_sell_clothes_v1.DTO.Shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a new shipping method
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethodCreateDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String estimatedDeliveryTime;

    @NotNull(message = "Base fee is required")
    @Positive(message = "Base fee must be positive")
    private BigDecimal baseFee;

    @Positive(message = "Extra fee per kg must be positive")
    private BigDecimal extraFeePerKg;
}