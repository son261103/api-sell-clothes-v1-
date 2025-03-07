package com.example.api_sell_clothes_v1.DTO.Shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for applying shipping to an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyShippingDTO {
    private Long orderId;
    private Long shippingMethodId;
    private Double totalWeight;
}