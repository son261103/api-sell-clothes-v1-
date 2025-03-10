package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Coupons.CouponValidationDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.*;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingEstimateDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Service.CouponService;
import com.example.api_sell_clothes_v1.Service.OrderItemService;
import com.example.api_sell_clothes_v1.Service.OrderService;
import com.example.api_sell_clothes_v1.Service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/orders")
@RequiredArgsConstructor
public class UserOrderController {
    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final ShippingService shippingService;
    private final CouponService couponService;

    /**
     * Tạo đơn hàng mới từ giỏ hàng
     */
    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @Valid @RequestBody CreateOrderDTO createDTO) {
        try {
            OrderResponseDTO createdOrder = orderService.createOrder(userId, createDTO);
            return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi tạo đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi tạo đơn hàng cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi tạo đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Ước tính phí vận chuyển cho đơn hàng
     */
    @GetMapping("/shipping-estimate")
    public ResponseEntity<?> estimateOrderShipping(
            @RequestParam BigDecimal orderTotal,
            @RequestParam Long shippingMethodId,
            @RequestParam(required = false) Double totalWeight,
            @RequestParam(required = false) String couponCode) {
        try {
            // Nếu có mã giảm giá, áp dụng giảm giá trước khi tính phí vận chuyển
            BigDecimal finalOrderTotal = orderTotal;
            BigDecimal discountAmount = BigDecimal.ZERO;

            if (couponCode != null && !couponCode.isEmpty()) {
                CouponValidationDTO validationResult = couponService.validateCoupon(couponCode, orderTotal);
                if (validationResult.isValid()) {
                    discountAmount = validationResult.getDiscountAmount();
                    finalOrderTotal = orderTotal.subtract(discountAmount);
                } else {
                    // Nếu mã không hợp lệ, vẫn tiếp tục tính toán nhưng thông báo
                    log.warn("Mã giảm giá không hợp lệ khi ước tính phí vận chuyển: {}", validationResult.getMessage());
                }
            }

            ShippingEstimateDTO estimate = shippingService.estimateShipping(finalOrderTotal, shippingMethodId, totalWeight);

            // Thêm thông tin giảm giá vào kết quả
            Map<String, Object> result = new HashMap<>();
            result.put("shippingEstimate", estimate);
            result.put("originalTotal", orderTotal);
            result.put("discountAmount", discountAmount);
            result.put("finalTotal", finalOrderTotal.add(estimate.getShippingFee()));
            result.put("finalTotalWithShipping", finalOrderTotal.add(estimate.getShippingFee()));

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi ước tính phí vận chuyển: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi ước tính phí vận chuyển: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi ước tính phí vận chuyển: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra và xác thực mã giảm giá
     */
    @GetMapping("/validate-coupon")
    public ResponseEntity<?> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam BigDecimal orderAmount) {
        try {
            CouponValidationDTO validationResult = couponService.validateCoupon(couponCode, orderAmount);
            return ResponseEntity.ok(validationResult);
        } catch (Exception e) {
            log.error("Lỗi khi xác thực mã giảm giá: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xác thực mã giảm giá: " + e.getMessage()));
        }
    }

    /**
     * Xem trước kết quả áp dụng mã giảm giá vào đơn hàng
     */
    @PostMapping("/preview-order")
    public ResponseEntity<?> previewOrder(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @Valid @RequestBody CreateOrderDTO createDTO) {
        try {
            // Tính tổng tiền hàng (chưa tính mã giảm giá và phí vận chuyển)
            BigDecimal subtotal = BigDecimal.ZERO;

            // Trong thực tế, bạn sẽ lấy thông tin giỏ hàng hoặc các sản phẩm đã chọn để tính toán
            // Đây là code mẫu, cần triển khai logic thực tế

            // Kiểm tra mã giảm giá nếu có
            BigDecimal discountAmount = BigDecimal.ZERO;

            if (createDTO.getCouponCode() != null && !createDTO.getCouponCode().isEmpty()) {
                CouponValidationDTO validationResult = couponService.validateCoupon(
                        createDTO.getCouponCode(), subtotal);

                if (validationResult.isValid()) {
                    discountAmount = validationResult.getDiscountAmount();
                }
            }

            // Tính phí vận chuyển
            BigDecimal afterDiscount = subtotal.subtract(discountAmount);
            BigDecimal shippingFee = BigDecimal.ZERO;

            if (createDTO.getShippingMethodId() != null) {
                ShippingEstimateDTO estimate = shippingService.estimateShipping(
                        afterDiscount,
                        createDTO.getShippingMethodId(),
                        createDTO.getTotalWeight());
                shippingFee = estimate.getShippingFee();
            }

            // Tổng hợp kết quả
            Map<String, Object> result = new HashMap<>();
            result.put("subtotal", subtotal);
            result.put("discountAmount", discountAmount);
            result.put("afterDiscount", afterDiscount);
            result.put("shippingFee", shippingFee);
            result.put("totalAmount", afterDiscount.add(shippingFee));

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi xem trước đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi xem trước đơn hàng: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xem trước đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách phương thức vận chuyển
     */
    @GetMapping("/shipping-methods")
    public ResponseEntity<?> getShippingMethods() {
        try {
            List<ShippingMethodDTO> methods = shippingService.getAllShippingMethods();
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách phương thức vận chuyển: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách phương thức vận chuyển: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin đơn hàng theo ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getUserOrderById(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PathVariable Long orderId) {
        try {
            OrderResponseDTO order = orderService.getUserOrderById(userId, orderId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy đơn hàng {} cho userId {}: {}", orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách đơn hàng của người dùng
     */
    @GetMapping
    public ResponseEntity<?> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        try {
            Page<OrderSummaryDTO> orders = orderService.getUserOrders(userId, pageable);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách đơn hàng cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách đơn hàng theo trạng thái
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getUserOrdersByStatus(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PathVariable Order.OrderStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        try {
            Page<OrderSummaryDTO> orders = orderService.getUserOrdersByStatus(userId, status, pageable);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách đơn hàng theo trạng thái {} cho userId {}: {}", status, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách đơn hàng theo trạng thái: " + e.getMessage()));
        }
    }

    /**
     * Hủy đơn hàng
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody CancelOrderDTO cancelDTO) {
        try {
            OrderResponseDTO cancelledOrder = orderService.cancelOrder(userId, orderId, cancelDTO);
            return ResponseEntity.ok(cancelledOrder);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi hủy đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi hủy đơn hàng {} cho userId {}: {}", orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi hủy đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Lấy chi tiết các sản phẩm trong đơn hàng
     */
    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PathVariable Long orderId) {
        try {
            // Kiểm tra xem đơn hàng có thuộc về người dùng không
            orderService.getUserOrderById(userId, orderId);
            List<OrderItemDTO> items = orderItemService.getOrderItems(orderId);
            return ResponseEntity.ok(items);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy chi tiết đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết đơn hàng {} cho userId {}: {}", orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy chi tiết đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin mã giảm giá đã áp dụng cho đơn hàng
     */
    @GetMapping("/{orderId}/coupons")
    public ResponseEntity<?> getOrderCoupons(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @PathVariable Long orderId) {
        try {
            // Kiểm tra xem đơn hàng có thuộc về người dùng không
            OrderResponseDTO order = orderService.getUserOrderById(userId, orderId);
            return ResponseEntity.ok(order.getCoupons());
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy thông tin mã giảm giá của đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin mã giảm giá của đơn hàng {} cho userId {}: {}",
                    orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy thông tin mã giảm giá của đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách sản phẩm bán chạy
     */
    @GetMapping("/bestselling")
    public ResponseEntity<List<BestsellingProductDTO>> getBestsellingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<BestsellingProductDTO> products = orderService.getBestsellingProducts(limit);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách sản phẩm bán chạy: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Kiểm tra điều kiện miễn phí vận chuyển
     */
    @GetMapping("/free-shipping-check")
    public ResponseEntity<?> checkFreeShippingEligibility(
            @RequestParam BigDecimal orderTotal,
            @RequestParam(required = false) String couponCode) {
        try {
            // Nếu có mã giảm giá, áp dụng giảm giá trước khi kiểm tra điều kiện miễn phí vận chuyển
            BigDecimal finalOrderTotal = orderTotal;
            BigDecimal discountAmount = BigDecimal.ZERO;

            if (couponCode != null && !couponCode.isEmpty()) {
                CouponValidationDTO validationResult = couponService.validateCoupon(couponCode, orderTotal);
                if (validationResult.isValid()) {
                    discountAmount = validationResult.getDiscountAmount();
                    finalOrderTotal = orderTotal.subtract(discountAmount);
                }
            }

            BigDecimal threshold = new BigDecimal("500000");
            boolean isEligible = finalOrderTotal.compareTo(threshold) >= 0;

            Map<String, Object> result = new HashMap<>();
            result.put("eligible", isEligible);
            result.put("threshold", threshold);
            result.put("originalTotal", orderTotal);
            result.put("discountAmount", discountAmount);
            result.put("finalTotal", finalOrderTotal);

            if (!isEligible) {
                result.put("amountNeeded", threshold.subtract(finalOrderTotal));
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra điều kiện miễn phí vận chuyển: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi kiểm tra điều kiện miễn phí vận chuyển: " + e.getMessage()));
        }
    }
}