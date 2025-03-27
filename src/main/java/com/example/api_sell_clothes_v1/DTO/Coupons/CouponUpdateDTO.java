package com.example.api_sell_clothes_v1.DTO.Coupons;

import com.example.api_sell_clothes_v1.Entity.Coupon;
import jakarta.validation.constraints.*;
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
public class CouponUpdateDTO {
    @Size(min = 3, max = 50, message = "Mã giảm giá phải từ 3-50 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]*$", message = "Mã giảm giá chỉ chấp nhận chữ in hoa, số và ký tự _ -")
    private String code;

    private Coupon.CouponType type;

    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal value;

    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0", message = "Giảm giá tối đa không được âm")
    private BigDecimal maxDiscountAmount;

    private Timestamp startDate;

    private Timestamp endDate;

    @Min(value = 1, message = "Giới hạn sử dụng phải lớn hơn 0")
    private Integer usageLimit;

    private Boolean status;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;
}
