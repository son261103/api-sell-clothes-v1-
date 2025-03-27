package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponValidationDTO;
import com.example.api_sell_clothes_v1.Service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_ORDER_COUPONS)
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    /**
     * Lấy tất cả mã giảm giá (phân trang)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_COUPON')")
    public ResponseEntity<Page<CouponResponseDTO>> getAllCoupons(
            @PageableDefault(page = 0, size = 10, sort = "code") Pageable pageable) {
        return ResponseEntity.ok(couponService.getAllCoupons(pageable));
    }

    /**
     * Tìm kiếm mã giảm giá
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_COUPON')")
    public ResponseEntity<Page<CouponResponseDTO>> searchCoupons(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) Boolean isExpired,
            @PageableDefault(page = 0, size = 10, sort = "code") Pageable pageable) {
        return ResponseEntity.ok(couponService.searchCoupons(code, status, isExpired, pageable));
    }

    /**
     * Lấy mã giảm giá theo ID
     */
    @GetMapping("/{couponId}")
    @PreAuthorize("hasAuthority('VIEW_COUPON')")
    public ResponseEntity<CouponResponseDTO> getCouponById(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.getCouponById(couponId));
    }

    /**
     * Lấy mã giảm giá theo mã code
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('VIEW_COUPON')")
    public ResponseEntity<CouponResponseDTO> getCouponByCode(@PathVariable String code) {
        return ResponseEntity.ok(couponService.getCouponByCode(code));
    }

    /**
     * Tạo mã giảm giá mới
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_COUPON')")
    public ResponseEntity<CouponResponseDTO> createCoupon(@Valid @RequestBody CouponCreateDTO createDTO) {
        try {
            CouponResponseDTO newCoupon = couponService.createCoupon(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCoupon);
        } catch (IllegalArgumentException e) {
            log.error("Lỗi khi tạo mã giảm giá: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Cập nhật mã giảm giá
     */
    @PutMapping("/{couponId}")
    @PreAuthorize("hasAuthority('EDIT_COUPON')")
    public ResponseEntity<CouponResponseDTO> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponUpdateDTO updateDTO) {
        try {
            CouponResponseDTO updatedCoupon = couponService.updateCoupon(couponId, updateDTO);
            return ResponseEntity.ok(updatedCoupon);
        } catch (IllegalArgumentException e) {
            log.error("Lỗi khi cập nhật mã giảm giá: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa mã giảm giá
     */
    @DeleteMapping("/{couponId}")
    @PreAuthorize("hasAuthority('DELETE_COUPON')")
    public ResponseEntity<ApiResponse> deleteCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.deleteCoupon(couponId));
    }

    /**
     * Kích hoạt/vô hiệu hóa mã giảm giá
     */
    @PutMapping("/{couponId}/toggle")
    @PreAuthorize("hasAuthority('EDIT_COUPON')")
    public ResponseEntity<CouponResponseDTO> toggleCouponStatus(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.toggleCouponStatus(couponId));
    }

    /**
     * Lấy danh sách mã giảm giá còn hiệu lực
     */
    @GetMapping("/valid")
    @PreAuthorize("hasAuthority('VIEW_COUPON')")
    public ResponseEntity<List<CouponResponseDTO>> getValidCoupons() {
        return ResponseEntity.ok(couponService.getValidCoupons());
    }

    /**
     * Kiểm tra mã giảm giá
     */
    @GetMapping("/validate")
    public ResponseEntity<CouponValidationDTO> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        return ResponseEntity.ok(couponService.validateCoupon(code, orderAmount));
    }

    /**
     * Lấy thống kê mã giảm giá
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('VIEW_COUPON')")
    public ResponseEntity<Map<String, Object>> getCouponStatistics() {
        return ResponseEntity.ok(couponService.getCouponStatistics());
    }

    /**
     * Lấy danh sách mã giảm giá công khai (cho người dùng)
     */
    @GetMapping("/public")
    public ResponseEntity<List<CouponResponseDTO>> getPublicCoupons() {
        return ResponseEntity.ok(couponService.getValidCoupons());
    }
}