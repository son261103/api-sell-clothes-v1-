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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
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
     * Xử lý callback từ VNPay và chuyển hướng về frontend React
     */
    @GetMapping("/vnpay-callback")
    public ResponseEntity<?> handleVnpayCallback(@RequestParam Map<String, String> vnpayParams) {
        try {
            log.info("Nhận callback từ VNPay: {}", vnpayParams);
            PaymentResponseDTO paymentResponse = paymentService.confirmPayment(vnpayParams);

            // Tạo URL chuyển hướng đến trang React với các tham số VNPay đã mã hóa
            String redirectUrl = "http://localhost:3001/payment/confirm?" + buildQueryString(vnpayParams);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(redirectUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 Redirect
        } catch (Exception e) {
            log.error("Lỗi khi xử lý callback VNPay: {}", e.getMessage(), e);
            String errorUrl;
            try {
                errorUrl = "http://localhost:3001/payment/error?message=" + URLEncoder.encode(e.getMessage(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                log.error("Lỗi mã hóa URL: {}", ex.getMessage(), ex);
                errorUrl = "http://localhost:3001/payment/error?message=Unknown%20error";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(errorUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }

    /**
     * Giữ endpoint confirm hiện tại (trả về JSON cho tương thích cũ)
     */
    @GetMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestParam Map<String, String> vnpayParams) {
        try {
            log.info("Nhận callback từ VNPay (endpoint cũ): {}", vnpayParams);
            PaymentResponseDTO paymentResponse = paymentService.confirmPayment(vnpayParams);
            return ResponseEntity.ok(new ApiResponse(true, "Thanh toán thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận thanh toán VNPay: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xác nhận thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Xác nhận giao hàng với OTP (cho người dùng)
     */
    @PostMapping("/confirm-delivery/{orderId}")
    public ResponseEntity<?> confirmDeliveryWithOtp(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long orderId,
            @RequestBody UserOtpConfirmRequest otpRequest) {
        try {
            PaymentResponseDTO result = paymentService.confirmDeliveryWithOtp(orderId, otpRequest.getOtp());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi xác nhận giao hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận giao hàng với OTP cho đơn hàng {} của userId {}: {}",
                    orderId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xác nhận giao hàng: " + e.getMessage()));
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

            // If no payment exists, return a 204 No Content response with a message
            if (paymentResponse == null) {
                return ResponseEntity.ok(
                        new ApiResponse(true, "No payment exists for this order yet")
                );
            }

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
            String token = (String) payloadData.get("token");
            if (token == null || !isValidWebhookToken(token)) {
                log.warn("Token webhook không hợp lệ");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ");
            }

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

    private boolean isValidWebhookToken(String token) {
        return token != null && token.length() > 10;
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (query.length() > 0) {
                    query.append("&");
                }
                String encodedValue = URLEncoder.encode(entry.getValue(), "UTF-8");
                query.append(entry.getKey()).append("=").append(encodedValue);
            }
            return query.toString();
        } catch (UnsupportedEncodingException e) {
            log.error("Lỗi mã hóa query string: {}", e.getMessage(), e);
            return ""; // Trả về chuỗi rỗng hoặc xử lý lỗi theo cách phù hợp
        }
    }
}

class UserOtpConfirmRequest {
    private String otp;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}