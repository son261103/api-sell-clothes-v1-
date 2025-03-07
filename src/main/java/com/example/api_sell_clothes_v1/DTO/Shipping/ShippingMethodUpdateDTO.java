package com.example.api_sell_clothes_v1.DTO.Shipping;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating an existing shipping method
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethodUpdateDTO {

    private String name;

    private String estimatedDeliveryTime;

    @Positive(message = "Base fee must be positive")
    private BigDecimal baseFee;

    @Positive(message = "Extra fee per kg must be positive")
    private BigDecimal extraFeePerKg;
}