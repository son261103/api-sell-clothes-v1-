package com.example.api_sell_clothes_v1.DTO.Coupons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCouponDTO {
    private Long orderId;
    private CouponDTO coupon;
    private BigDecimal discountAmount;
}
