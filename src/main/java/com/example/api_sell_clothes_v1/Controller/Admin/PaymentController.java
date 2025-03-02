package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentRequestDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PAYMENT)
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * Tạo một giao dịch thanh toán cho đơn hàng (cho người dùng)
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_PAYMENT')")
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody PaymentRequestDTO paymentRequestDTO) {
        try {
            // Đảm bảo đơn hàng thuộc về người dùng (kiểm tra này có thể thực hiện trong service)
            PaymentResponseDTO paymentResponse = paymentService.createPayment(paymentRequestDTO);
            return new ResponseEntity<>(paymentResponse, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating payment for user {}: {}", userId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi tạo thanh toán: " + e.getMessage());
        }
    }

    /**
     * Xác nhận thanh toán VNPay (callback từ VNPay, không yêu cầu xác thực)
     */
    @GetMapping("/confirm")
    public ResponseEntity<PaymentResponseDTO> confirmPayment(@RequestParam Map<String, String> vnpayParams) {
        try {
            PaymentResponseDTO paymentResponse = paymentService.confirmPayment(vnpayParams);
            return ResponseEntity.ok(paymentResponse);
        } catch (Exception e) {
            log.error("Error confirming VNPay payment: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi xác nhận thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết thanh toán theo ID đơn hàng (cho người dùng)
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENT')")
    public ResponseEntity<PaymentResponseDTO> getPaymentByOrderId(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long orderId) {
        try {
            PaymentResponseDTO paymentResponse = paymentService.getPaymentByOrderId(orderId, userId);
            return ResponseEntity.ok(paymentResponse);
        } catch (Exception e) {
            log.error("Error fetching payment for order {} by user {}: {}", orderId, userId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy thông tin thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết thanh toán theo ID đơn hàng (cho admin)
     */
    @GetMapping("/admin/order/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT')")
    public ResponseEntity<PaymentResponseDTO> getPaymentByOrderIdAdmin(@PathVariable Long orderId) {
        try {
            PaymentResponseDTO paymentResponse = paymentService.getPaymentByOrderId(orderId, null);
            return ResponseEntity.ok(paymentResponse);
        } catch (Exception e) {
            log.error("Error fetching payment for order {} by admin: {}", orderId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy thông tin thanh toán: " + e.getMessage());
        }
    }
}