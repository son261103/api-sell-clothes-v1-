package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentMethodDTO;
import com.example.api_sell_clothes_v1.Service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/payment-methods")
@RequiredArgsConstructor
public class UserPaymentMethodController {
    private final PaymentMethodService paymentMethodService;

    /**
     * Lấy danh sách phương thức thanh toán khả dụng
     */
    @GetMapping
    public ResponseEntity<?> getActivePaymentMethods() {
        try {
            List<PaymentMethodDTO> paymentMethods = paymentMethodService.getActivePaymentMethods();
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách phương thức thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách phương thức thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin chi tiết phương thức thanh toán
     */
    @GetMapping("/{methodId}")
    public ResponseEntity<?> getPaymentMethodById(@PathVariable Long methodId) {
        try {
            PaymentMethodDTO paymentMethod = paymentMethodService.getPaymentMethodById(methodId);
            // Chỉ hiển thị nếu phương thức thanh toán đang hoạt động
            if (paymentMethod.getStatus()) {
                return ResponseEntity.ok(paymentMethod);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Phương thức thanh toán không khả dụng"));
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin phương thức thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi khi lấy thông tin phương thức thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra xem phương thức thanh toán có hỗ trợ ngân hàng không (VNPay)
     */
    @GetMapping("/{methodId}/banks")
    public ResponseEntity<?> getSupportedBanks(@PathVariable Long methodId) {
        try {
            // Kiểm tra phương thức có phải VNPay không
            PaymentMethodDTO paymentMethod = paymentMethodService.getPaymentMethodById(methodId);

            if ("VNPAY".equalsIgnoreCase(paymentMethod.getCode())) {
                List<String> supportedBanks = paymentMethodService.getSupportedBanks(methodId);
                return ResponseEntity.ok(supportedBanks);
            } else {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Phương thức thanh toán này không hỗ trợ danh sách ngân hàng"));
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách ngân hàng hỗ trợ: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách ngân hàng hỗ trợ: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra phương thức thanh toán có hiện đang bảo trì không
     */
    @GetMapping("/{methodId}/status")
    public ResponseEntity<?> checkPaymentMethodStatus(@PathVariable Long methodId) {
        try {
            boolean isActive = paymentMethodService.isPaymentMethodActive(methodId);
            return ResponseEntity.ok(new ApiResponse(isActive,
                    isActive ? "Phương thức thanh toán đang hoạt động" : "Phương thức thanh toán đang bảo trì"));
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trạng thái phương thức thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Lỗi khi kiểm tra trạng thái phương thức thanh toán: " + e.getMessage()));
        }
    }
}