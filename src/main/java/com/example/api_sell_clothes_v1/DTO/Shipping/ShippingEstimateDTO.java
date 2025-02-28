package com.example.api_sell_clothes_v1.DTO.Shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for shipping cost estimate
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingEstimateDTO {
    private Long methodId;
    private String methodName;
    private BigDecimal shippingFee;
    private String estimatedDeliveryTime;
    private boolean freeShippingEligible;
    private BigDecimal freeShippingThreshold;
}
