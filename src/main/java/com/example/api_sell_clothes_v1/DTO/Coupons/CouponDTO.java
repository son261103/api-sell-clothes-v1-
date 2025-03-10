package com.example.api_sell_clothes_v1.DTO.Coupons;

import com.example.api_sell_clothes_v1.Entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponDTO {
    private String code;
    private Coupon.CouponType type;
    private BigDecimal discountAmount;
}