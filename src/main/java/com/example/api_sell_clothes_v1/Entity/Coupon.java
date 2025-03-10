package com.example.api_sell_clothes_v1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CouponType type;

    @Column(name = "value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 15, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "end_date")
    private Timestamp endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", columnDefinition = "int unsigned default 0")
    private Integer usedCount;

    @Column(name = "status", columnDefinition = "tinyint(1) default 1")
    private Boolean status;

    // Thêm trường mô tả
    @Column(name = "description", length = 255)
    private String description;

    // Quan hệ đến OrderCoupon
    @OneToMany(mappedBy = "coupon")
    private List<OrderCoupon> orderCoupons = new ArrayList<>();

    public enum CouponType {
        percentage, fixed_amount
    }
}