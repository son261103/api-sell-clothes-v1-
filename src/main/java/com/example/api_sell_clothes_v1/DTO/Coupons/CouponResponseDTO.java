package com.example.api_sell_clothes_v1.DTO.Coupons;

import com.example.api_sell_clothes_v1.Entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDTO {
    private Long couponId;
    private String code;
    private Coupon.CouponType type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Timestamp startDate;
    private Timestamp endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean status;
    private String description;
    private Boolean isExpired;
    private Boolean isFullyUsed;
}