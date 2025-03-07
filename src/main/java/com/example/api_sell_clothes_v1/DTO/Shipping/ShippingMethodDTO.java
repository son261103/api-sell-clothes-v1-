package com.example.api_sell_clothes_v1.DTO.Shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for ShippingMethod entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethodDTO {
    private Long id;
    private String name;
    private String estimatedDeliveryTime;
    private BigDecimal baseFee;
    private BigDecimal extraFeePerKg;
}