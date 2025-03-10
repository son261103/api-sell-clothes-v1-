package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponValidationDTO;
import com.example.api_sell_clothes_v1.Service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/coupons")
@RequiredArgsConstructor
public class UserCouponController {
    private final CouponService couponService;

    /**
     * Lấy danh sách mã giảm giá có hiệu lực
     */
    @GetMapping
    public ResponseEntity<?> getValidCoupons() {
        try {
            List<CouponResponseDTO> coupons = couponService.getValidCoupons();
            return ResponseEntity.ok(coupons);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách mã giảm giá: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách mã giảm giá: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra và xác thực mã giảm giá
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        try {
            CouponValidationDTO validationResult = couponService.validateCoupon(code, orderAmount);
            return ResponseEntity.ok(validationResult);
        } catch (Exception e) {
            log.error("Lỗi khi xác thực mã giảm giá: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xác thực mã giảm giá: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin chi tiết mã giảm giá theo code
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getCouponDetails(@PathVariable String code) {
        try {
            CouponResponseDTO coupon = couponService.getCouponByCode(code);
            return ResponseEntity.ok(coupon);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy thông tin mã giảm giá: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin mã giảm giá: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy thông tin mã giảm giá: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra xem mã giảm giá có tồn tại không (quick check)
     */
    @GetMapping("/check/{code}")
    public ResponseEntity<?> checkCouponExists(@PathVariable String code) {
        try {
            boolean exists = couponService.getCouponByCode(code) != null;
            return ResponseEntity.ok(new ApiResponse(exists,
                    exists ? "Mã giảm giá hợp lệ" : "Mã giảm giá không tồn tại"));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse(false, "Mã giảm giá không tồn tại"));
        }
    }
}