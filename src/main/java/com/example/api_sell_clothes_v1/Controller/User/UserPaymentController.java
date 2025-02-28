package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentHistoryDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentRequestDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.Service.PaymentHistoryService;
import com.example.api_sell_clothes_v1.Service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/payment")
@RequiredArgsConstructor
public class UserPaymentController {
    private final PaymentService paymentService;
    private final PaymentHistoryService paymentHistoryService;

    /**
     * Tạo giao dịch thanh toán cho đơn hàng
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPayment(
            @RequestHeader(value = "X-User-Id") Long userId,
            @Valid @RequestBody PaymentRequestDTO paymentRequestDTO) {
        try {
            // Thêm userId vào DTO để kiểm tra quyền sở hữu
            paymentRequestDTO.setUserId(userId);
            PaymentResponseDTO paymentResponse = paymentService.createPayment(paymentRequestDTO);
            return new ResponseEntity<>(paymentResponse, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi tạo thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi tạo thanh toán cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi tạo thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin chi tiết thanh toán theo ID đơn hàng
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long orderId) {
        try {
            PaymentResponseDTO paymentResponse = paymentService.getPaymentByOrderId(orderId, userId);
            return ResponseEntity.ok(paymentResponse);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy thông tin thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin thanh toán cho đơn hàng {} của userId {}: {}",
                    orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy thông tin thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Hủy giao dịch thanh toán
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<?> cancelPayment(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long paymentId) {
        try {
            ApiResponse response = paymentService.cancelPayment(paymentId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi hủy thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi hủy thanh toán {} của userId {}: {}",
                    paymentId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi hủy thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<?> checkPaymentStatus(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable String transactionId) {
        try {
            String status = paymentService.checkPaymentStatusWithGateway(transactionId);
            return ResponseEntity.ok(new ApiResponse(true, status));
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trạng thái thanh toán: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy lịch sử thanh toán theo ID đơn hàng
     */
    @GetMapping("/history/order/{orderId}")
    public ResponseEntity<?> getPaymentHistoryByOrderId(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long orderId) {
        try {
            List<PaymentHistoryDTO> history = paymentHistoryService.getPaymentHistoryByOrderId(orderId, userId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy lịch sử thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử thanh toán cho đơn hàng {} của userId {}: {}",
                    orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy lịch sử thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Xác nhận thanh toán VNPay (callback từ VNPay, không yêu cầu xác thực)
     */
    @GetMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestParam Map<String, String> vnpayParams) {
        try {
            log.info("Nhận callback từ VNPay: {}", vnpayParams);
            PaymentResponseDTO paymentResponse = paymentService.confirmPayment(vnpayParams);

            // Chuyển hướng người dùng đến trang thông báo thành công
            return ResponseEntity.ok(new ApiResponse(true, "Thanh toán thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận thanh toán VNPay: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xác nhận thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Chuyển hướng đến URL thanh toán đầy đủ
     */
    @GetMapping("/payment-redirect/{orderId}")
    public ResponseEntity<?> redirectToPayment(@PathVariable Long orderId) {
        try {
            return paymentService.redirectToPayment(orderId);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển hướng thanh toán: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi chuyển hướng thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Webhook endpoint để nhận thông báo từ cổng thanh toán
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> paymentWebhook(@RequestBody Map<String, Object> payloadData) {
        try {
            log.info("Nhận webhook thanh toán: {}", payloadData);

            // Xác thực webhook
            String token = (String) payloadData.get("token");
            if (token == null || !isValidWebhookToken(token)) {
                log.warn("Token webhook không hợp lệ");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ");
            }

            // Xử lý dữ liệu webhook
            String transactionId = (String) payloadData.get("transactionId");
            String status = (String) payloadData.get("status");

            if (transactionId != null && "success".equals(status)) {
                log.info("Xử lý webhook thanh toán thành công cho giao dịch: {}", transactionId);
                paymentService.handleWebhookPaymentSuccess(transactionId);
            } else if (transactionId != null && "failed".equals(status)) {
                log.info("Xử lý webhook thanh toán thất bại cho giao dịch: {}", transactionId);
                String failureReason = (String) payloadData.getOrDefault("reason", "Thanh toán thất bại (webhook)");
                paymentService.handleWebhookPaymentFailure(transactionId, failureReason);
            }

            return ResponseEntity.ok("Webhook đã được xử lý thành công");
        } catch (Exception e) {
            log.error("Lỗi khi xử lý webhook thanh toán: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý webhook: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra tính hợp lệ của token webhook
     */
    private boolean isValidWebhookToken(String token) {
        // Thực hiện xác thực token, ví dụ:
        // - So sánh với mã bí mật được cấu hình trong ứng dụng
        // - Xác thực chữ ký số
        // Đây chỉ là giả lập đơn giản
        return token != null && token.length() > 10;
    }
}