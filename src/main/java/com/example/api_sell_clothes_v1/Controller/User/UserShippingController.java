package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingEstimateDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodDTO;
import com.example.api_sell_clothes_v1.Service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/shipping-methods")
@RequiredArgsConstructor
public class UserShippingController {
    private final ShippingService shippingService;

    /**
     * Lấy tất cả phương thức vận chuyển
     */
    @GetMapping("")
    public ResponseEntity<?> getAllShippingMethods() {
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
     * Lấy phương thức vận chuyển theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getShippingMethodById(@PathVariable Long id) {
        try {
            ShippingMethodDTO method = shippingService.getShippingMethodById(id);
            return ResponseEntity.ok(method);
        } catch (Exception e) {
            log.error("Lỗi khi lấy phương thức vận chuyển với ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy phương thức vận chuyển: " + e.getMessage()));
        }
    }

    /**
     * Ước tính phí vận chuyển cho giỏ hàng
     */
    @GetMapping("/estimate")
    public ResponseEntity<?> estimateShipping(
            @RequestParam BigDecimal orderTotal,
            @RequestParam Long methodId,
            @RequestParam(required = false) Double totalWeight) {
        try {
            ShippingEstimateDTO estimate = shippingService.estimateShipping(orderTotal, methodId, totalWeight);
            return ResponseEntity.ok(estimate);
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
     * Ước tính phí vận chuyển cho người dùng đã đăng nhập
     */
    @GetMapping("/user-estimate")
    public ResponseEntity<?> estimateShippingForUser(
            @RequestHeader(value = "X-User-Id", required = true) Long userId,
            @RequestParam BigDecimal orderTotal,
            @RequestParam Long methodId,
            @RequestParam(required = false) Double totalWeight) {
        try {
            // Ở đây bạn có thể mở rộng để tính toán trọng lượng dựa trên giỏ hàng của người dùng
            // hoặc lấy địa chỉ mặc định của người dùng để tính phí vận chuyển chính xác hơn
            ShippingEstimateDTO estimate = shippingService.estimateShipping(orderTotal, methodId, totalWeight);
            return ResponseEntity.ok(estimate);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi ước tính phí vận chuyển cho người dùng {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi ước tính phí vận chuyển cho người dùng {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi ước tính phí vận chuyển: " + e.getMessage()));
        }
    }
}