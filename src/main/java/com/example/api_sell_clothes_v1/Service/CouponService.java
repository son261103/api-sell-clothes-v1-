package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Coupons.*;
import com.example.api_sell_clothes_v1.Entity.Coupon;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.OrderCoupon;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.CouponMapper;
import com.example.api_sell_clothes_v1.Repository.CouponRepository;
import com.example.api_sell_clothes_v1.Repository.OrderCouponRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final CouponMapper couponMapper;

    /**
     * Lấy tất cả mã giảm giá với phân trang
     */
    @Transactional(readOnly = true)
    public Page<CouponResponseDTO> getAllCoupons(Pageable pageable) {
        Page<Coupon> coupons = couponRepository.findAll(pageable);
        return coupons.map(couponMapper::toDto);
    }

    /**
     * Tìm kiếm mã giảm giá theo các tiêu chí
     */
    @Transactional(readOnly = true)
    public Page<CouponResponseDTO> searchCoupons(
            String code, Boolean status, Boolean isExpired, Pageable pageable) {
        Page<Coupon> coupons = couponRepository.searchCoupons(code, status, isExpired, pageable);
        return coupons.map(couponMapper::toDto);
    }

    /**
     * Lấy thông tin mã giảm giá theo ID
     */
    @Transactional(readOnly = true)
    public CouponResponseDTO getCouponById(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
        return couponMapper.toDto(coupon);
    }

    /**
     * Lấy thông tin mã giảm giá theo mã code
     */
    @Transactional(readOnly = true)
    public CouponResponseDTO getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));
        return couponMapper.toDto(coupon);
    }

    /**
     * Tạo mã giảm giá mới
     */
    @Transactional
    public CouponResponseDTO createCoupon(CouponCreateDTO createDTO) {
        // Kiểm tra xem mã đã tồn tại chưa
        if (couponRepository.findByCode(createDTO.getCode().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại");
        }

        // Kiểm tra các ràng buộc logic
        validateCouponLogic(createDTO.getType(), createDTO.getValue(), createDTO.getMaxDiscountAmount());

        // Tạo entity và lưu
        Coupon coupon = couponMapper.toEntity(createDTO);
        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Đã tạo mã giảm giá mới: {}", savedCoupon.getCode());

        return couponMapper.toDto(savedCoupon);
    }

    /**
     * Cập nhật mã giảm giá
     */
    @Transactional
    public CouponResponseDTO updateCoupon(Long couponId, CouponUpdateDTO updateDTO) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        // Kiểm tra nếu đổi code thì code mới không được trùng
        if (updateDTO.getCode() != null && !updateDTO.getCode().equalsIgnoreCase(coupon.getCode())) {
            couponRepository.findByCode(updateDTO.getCode().toUpperCase()).ifPresent(c -> {
                throw new IllegalArgumentException("Mã giảm giá này đã tồn tại");
            });
        }

        // Kiểm tra các ràng buộc logic
        Coupon.CouponType type = updateDTO.getType() != null ? updateDTO.getType() : coupon.getType();
        BigDecimal value = updateDTO.getValue() != null ? updateDTO.getValue() : coupon.getValue();
        BigDecimal maxDiscount = updateDTO.getMaxDiscountAmount() != null ?
                updateDTO.getMaxDiscountAmount() : coupon.getMaxDiscountAmount();

        validateCouponLogic(type, value, maxDiscount);

        // Cập nhật entity
        couponMapper.updateEntityFromDto(updateDTO, coupon);
        Coupon updatedCoupon = couponRepository.save(coupon);
        log.info("Đã cập nhật mã giảm giá: {}", updatedCoupon.getCode());

        return couponMapper.toDto(updatedCoupon);
    }

    /**
     * Xóa mã giảm giá
     */
    @Transactional
    public ApiResponse deleteCoupon(Long couponId) {
        try {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

            // Kiểm tra xem mã giảm giá đã được sử dụng chưa
            int usageCount = orderCouponRepository.countUsagesByCouponId(couponId);
            if (usageCount > 0) {
                return new ApiResponse(false, "Không thể xóa mã giảm giá đã được sử dụng");
            }

            couponRepository.delete(coupon);
            log.info("Đã xóa mã giảm giá: {}", coupon.getCode());

            return new ApiResponse(true, "Đã xóa mã giảm giá thành công");
        } catch (DataIntegrityViolationException e) {
            log.error("Lỗi xóa mã giảm giá: {}", e.getMessage());
            return new ApiResponse(false, "Không thể xóa mã giảm giá đã được sử dụng");
        }
    }

    /**
     * Lấy danh sách mã giảm giá hợp lệ (còn hạn, còn lượt dùng)
     */
    @Transactional(readOnly = true)
    public List<CouponResponseDTO> getValidCoupons() {
        List<Coupon> validCoupons = couponRepository.findAllValidCoupons();
        return couponMapper.toDto(validCoupons);
    }

    /**
     * Kích hoạt/Vô hiệu hóa mã giảm giá
     */
    @Transactional
    public CouponResponseDTO toggleCouponStatus(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        coupon.setStatus(!coupon.getStatus());
        Coupon updatedCoupon = couponRepository.save(coupon);

        String statusMessage = updatedCoupon.getStatus() ? "kích hoạt" : "vô hiệu hóa";
        log.info("Đã {} mã giảm giá: {}", statusMessage, coupon.getCode());

        return couponMapper.toDto(updatedCoupon);
    }

    /**
     * Kiểm tra tính hợp lệ của mã giảm giá
     */
    @Transactional(readOnly = true)
    public CouponValidationDTO validateCoupon(String code, BigDecimal orderAmount) {
        try {
            Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                    .orElseThrow(() -> new EntityNotFoundException("Mã giảm giá không tồn tại"));

            // Kiểm tra trạng thái
            if (!coupon.getStatus()) {
                return CouponValidationDTO.builder()
                        .valid(false)
                        .message("Mã giảm giá đã bị vô hiệu hóa")
                        .build();
            }

            // Kiểm tra thời hạn
            Timestamp now = new Timestamp(System.currentTimeMillis());
            if (coupon.getStartDate() != null && now.before(coupon.getStartDate())) {
                return CouponValidationDTO.builder()
                        .valid(false)
                        .message("Mã giảm giá chưa đến thời gian sử dụng")
                        .build();
            }

            if (coupon.getEndDate() != null && now.after(coupon.getEndDate())) {
                return CouponValidationDTO.builder()
                        .valid(false)
                        .message("Mã giảm giá đã hết hạn")
                        .build();
            }

            // Kiểm tra số lượt sử dụng
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                return CouponValidationDTO.builder()
                        .valid(false)
                        .message("Mã giảm giá đã hết lượt sử dụng")
                        .build();
            }

            // Kiểm tra giá trị đơn hàng tối thiểu
            if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
                return CouponValidationDTO.builder()
                        .valid(false)
                        .message("Đơn hàng chưa đạt giá trị tối thiểu " +
                                formatCurrency(coupon.getMinOrderAmount()) + " để sử dụng mã giảm giá")
                        .build();
            }

            // Tính toán số tiền được giảm
            BigDecimal discountAmount = calculateDiscount(coupon, orderAmount);

            return CouponValidationDTO.builder()
                    .valid(true)
                    .message("Mã giảm giá hợp lệ")
                    .discountAmount(discountAmount)
                    .coupon(couponMapper.toDto(coupon))
                    .build();

        } catch (EntityNotFoundException e) {
            return CouponValidationDTO.builder()
                    .valid(false)
                    .message("Mã giảm giá không tồn tại")
                    .build();
        } catch (Exception e) {
            log.error("Lỗi kiểm tra mã giảm giá: {}", e.getMessage());
            return CouponValidationDTO.builder()
                    .valid(false)
                    .message("Lỗi hệ thống, vui lòng thử lại sau")
                    .build();
        }
    }

    /**
     * Áp dụng mã giảm giá vào đơn hàng
     */
    @Transactional
    public void applyCouponToOrder(Order order, Coupon coupon, BigDecimal discountAmount) {
        // Tạo OrderCoupon
        OrderCoupon orderCoupon = new OrderCoupon();
        orderCoupon.setOrder(order);
        orderCoupon.setCoupon(coupon);
        orderCoupon.setDiscountAmount(discountAmount);

        // Lưu vào database
        orderCouponRepository.save(orderCoupon);

        // Tăng số lần sử dụng của coupon
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        log.info("Đã áp dụng mã giảm giá {} cho đơn hàng {}, giảm giá: {}",
                coupon.getCode(), order.getOrderId(), formatCurrency(discountAmount));
    }

    /**
     * Tính toán số tiền được giảm
     */
    public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;

        if (coupon.getType() == Coupon.CouponType.percentage) {
            // Nếu là giảm giá theo phần trăm
            discount = orderAmount.multiply(coupon.getValue().divide(new BigDecimal("100")));

            // Áp dụng giới hạn giảm giá tối đa (nếu có)
            if (coupon.getMaxDiscountAmount() != null &&
                    discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        } else {
            // Nếu là giảm giá cố định
            discount = coupon.getValue();

            // Đảm bảo discount không lớn hơn giá trị đơn hàng
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
        }

        return discount;
    }

    /**
     * Lấy thống kê mã giảm giá
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCouponStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        long totalCoupons = couponRepository.count();
        long activeCoupons = couponRepository.countActiveCoupons();
        long expiredCoupons = couponRepository.countExpiredCoupons();
        long fullyUsedCoupons = couponRepository.countFullyUsedCoupons();

        statistics.put("totalCoupons", totalCoupons);
        statistics.put("activeCoupons", activeCoupons);
        statistics.put("expiredCoupons", expiredCoupons);
        statistics.put("fullyUsedCoupons", fullyUsedCoupons);

        return statistics;
    }

    /**
     * Hủy bỏ áp dụng mã giảm giá cho đơn hàng
     */
    @Transactional
    public void removeCouponsFromOrder(Long orderId) {
        orderCouponRepository.deleteByOrderOrderId(orderId);
        log.info("Đã hủy tất cả mã giảm giá cho đơn hàng: {}", orderId);
    }

    /**
     * Helper methods
     */
    private void validateCouponLogic(Coupon.CouponType type, BigDecimal value, BigDecimal maxDiscountAmount) {
        // Nếu là mã giảm theo phần trăm
        if (type == Coupon.CouponType.percentage) {
            // Giá trị phần trăm phải từ 0 đến 100
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Giá trị phần trăm giảm giá phải từ 0 đến 100");
            }
        }

        // Nếu có giá trị giảm tối đa, phải lớn hơn 0
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm tối đa phải lớn hơn 0");
        }
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}
