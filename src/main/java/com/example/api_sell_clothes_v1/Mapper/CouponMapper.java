package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Coupons.CouponCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.Coupon;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CouponMapper {
    public CouponResponseDTO toDto(Coupon coupon) {
        if (coupon == null) {
            return null;
        }

        // Tính toán trạng thái hết hạn
        boolean isExpired = false;
        if (coupon.getEndDate() != null) {
            isExpired = coupon.getEndDate().before(new Timestamp(System.currentTimeMillis()));
        }

        // Tính toán trạng thái hết lượt sử dụng
        boolean isFullyUsed = false;
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() != null) {
            isFullyUsed = coupon.getUsedCount() >= coupon.getUsageLimit();
        }

        return CouponResponseDTO.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .status(coupon.getStatus())
                .description(coupon.getDescription())
                .isExpired(isExpired)
                .isFullyUsed(isFullyUsed)
                .build();
    }

    public List<CouponResponseDTO> toDto(List<Coupon> coupons) {
        return coupons.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Coupon toEntity(CouponCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        return Coupon.builder()
                .code(dto.getCode().toUpperCase())
                .type(dto.getType())
                .value(dto.getValue())
                .minOrderAmount(dto.getMinOrderAmount())
                .maxDiscountAmount(dto.getMaxDiscountAmount())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .usedCount(0)
                .status(dto.getStatus() != null ? dto.getStatus() : true)
                .description(dto.getDescription())
                .build();
    }

    public void updateEntityFromDto(CouponUpdateDTO dto, Coupon entity) {
        if (dto == null) {
            return;
        }

        if (dto.getCode() != null) {
            entity.setCode(dto.getCode().toUpperCase());
        }

        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }

        if (dto.getValue() != null) {
            entity.setValue(dto.getValue());
        }

        if (dto.getMinOrderAmount() != null) {
            entity.setMinOrderAmount(dto.getMinOrderAmount());
        }

        if (dto.getMaxDiscountAmount() != null) {
            entity.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        }

        if (dto.getStartDate() != null) {
            entity.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            entity.setEndDate(dto.getEndDate());
        }

        if (dto.getUsageLimit() != null) {
            entity.setUsageLimit(dto.getUsageLimit());
        }

        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
    }
}