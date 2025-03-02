package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentHistoryDTO;
import com.example.api_sell_clothes_v1.Service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PAYMENT_HISTORY)
@RequiredArgsConstructor
public class PaymentHistoryController {
    private final PaymentHistoryService paymentHistoryService;

    /**
     * Lấy lịch sử thanh toán theo ID thanh toán (cho người dùng)
     */
    @GetMapping("/payment/{paymentId}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENT_HISTORY')")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistoryByPaymentId(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long paymentId) {
        try {
            List<PaymentHistoryDTO> history = paymentHistoryService.getPaymentHistoryByPaymentId(paymentId, userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payment history for payment {} by user {}: {}", paymentId, userId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy lịch sử thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử thanh toán theo ID đơn hàng (cho người dùng)
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENT_HISTORY')")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistoryByOrderId(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long orderId) {
        try {
            List<PaymentHistoryDTO> history = paymentHistoryService.getPaymentHistoryByOrderId(orderId, userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payment history for order {} by user {}: {}", orderId, userId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy lịch sử thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử thanh toán theo ID thanh toán (cho admin)
     */
    @GetMapping("/admin/payment/{paymentId}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_HISTORY')")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistoryByPaymentIdAdmin(
            @PathVariable Long paymentId) {
        try {
            List<PaymentHistoryDTO> history = paymentHistoryService.getPaymentHistoryByPaymentId(paymentId, null);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payment history for payment {} by admin: {}", paymentId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy lịch sử thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch sử thanh toán theo ID đơn hàng (cho admin)
     */
    @GetMapping("/admin/order/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_HISTORY')")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistoryByOrderIdAdmin(
            @PathVariable Long orderId) {
        try {
            List<PaymentHistoryDTO> history = paymentHistoryService.getPaymentHistoryByOrderId(orderId, null);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payment history for order {} by admin: {}", orderId, e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy lịch sử thanh toán: " + e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ lịch sử thanh toán phân trang (cho admin)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENT_HISTORY')")
    public ResponseEntity<Page<PaymentHistoryDTO>> getAllPaymentHistory(
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        try {
            Page<PaymentHistoryDTO> history = paymentHistoryService.getAllPaymentHistory(pageable);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching all payment history: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi lấy danh sách lịch sử thanh toán: " + e.getMessage());
        }
    }
}