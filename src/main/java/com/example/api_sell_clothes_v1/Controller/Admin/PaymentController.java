package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Payment;
import com.example.api_sell_clothes_v1.Service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PAYMENT + "/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('VIEW_PAYMENT')")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Lấy danh sách tất cả các thanh toán
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
        log.info("Nhận yêu cầu GET để lấy danh sách tất cả các thanh toán");
        try {
            List<PaymentResponseDTO> payments = paymentService.getAllPayments();
            log.info("Đã lấy thành công {} thanh toán từ hệ thống", payments.size());
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách tất cả thanh toán: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Lấy thông tin thanh toán theo orderId (dành cho admin, không cần kiểm tra userId)
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByOrderId(@PathVariable Long orderId) {
        log.info("Nhận yêu cầu GET để lấy thông tin thanh toán cho orderId: {}", orderId);
        try {
            PaymentResponseDTO paymentResponse = paymentService.getPaymentByOrderId(orderId, null);
            if (paymentResponse == null) {
                log.warn("Không tìm thấy thông tin thanh toán cho orderId: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            log.info("Đã lấy thành công thông tin thanh toán cho orderId: {}", orderId);
            return ResponseEntity.ok(paymentResponse);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin thanh toán cho orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Lấy thông tin thanh toán theo transactionCode
     */
    @GetMapping("/transaction/{transactionCode}")
    public ResponseEntity<PaymentResponseDTO> getPaymentByTransactionCode(@PathVariable String transactionCode) {
        log.info("Nhận yêu cầu GET để lấy thông tin thanh toán cho transactionCode: {}", transactionCode);
        try {
            PaymentResponseDTO paymentResponse = paymentService.getPaymentByTransactionCode(transactionCode);
            if (paymentResponse == null) {
                log.warn("Không tìm thấy thông tin thanh toán cho transactionCode: {}", transactionCode);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            log.info("Đã lấy thành công thông tin thanh toán cho transactionCode: {}", transactionCode);
            return ResponseEntity.ok(paymentResponse);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thông tin thanh toán theo transactionCode {}: {}", transactionCode, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Lấy danh sách các thanh toán PENDING quá hạn
     */
    @GetMapping("/pending/overdue")
    public ResponseEntity<List<PaymentResponseDTO>> getPendingPaymentsOlderThan(
            @RequestParam(value = "minutes", defaultValue = "30") int minutes) {
        log.info("Nhận yêu cầu GET để lấy danh sách thanh toán PENDING quá hạn với ngưỡng {} phút", minutes);
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);
            log.debug("Ngưỡng thời gian tính toán: {}", threshold);
            List<PaymentResponseDTO> overduePayments = paymentService.getPendingPaymentsOlderThan(threshold);
            log.info("Đã lấy thành công {} thanh toán PENDING quá hạn", overduePayments.size());
            return ResponseEntity.ok(overduePayments);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thanh toán PENDING quá hạn: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Cập nhật trạng thái thanh toán thủ công
     */
    @PutMapping("/{paymentId}/status")
    public ResponseEntity<PaymentResponseDTO> updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestBody PaymentStatusRequest statusRequest) {
        log.info("Nhận yêu cầu PUT để cập nhật trạng thái thanh toán cho paymentId: {} thành {}",
                paymentId, statusRequest.getStatus());
        try {
            PaymentResponseDTO updatedPayment = paymentService.updatePaymentStatus(paymentId, statusRequest.getStatus());
            log.info("Đã cập nhật thành công trạng thái thanh toán cho paymentId: {}", paymentId);
            return ResponseEntity.ok(updatedPayment);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật trạng thái thanh toán cho paymentId {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Xác nhận thanh toán COD
     */
    @PostMapping("/{paymentId}/confirm-cod")
    public ResponseEntity<PaymentResponseDTO> confirmCodPayment(
            @PathVariable Long paymentId,
            @RequestBody CodConfirmRequest confirmRequest) {
        log.info("Nhận yêu cầu POST để xác nhận thanh toán COD cho paymentId: {}, ghi chú: {}",
                paymentId, confirmRequest.getNote());
        try {
            PaymentResponseDTO updatedPayment = paymentService.confirmCodPayment(paymentId, confirmRequest.getNote());
            log.info("Đã xác nhận thành công thanh toán COD cho paymentId: {}", paymentId);
            return ResponseEntity.ok(updatedPayment);
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận thanh toán COD cho paymentId {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Xử lý từ chối giao hàng COD
     */
    @PostMapping("/{paymentId}/reject-cod")
    public ResponseEntity<PaymentResponseDTO> rejectCodPayment(
            @PathVariable Long paymentId,
            @RequestBody CodRejectionRequest rejectionRequest) {
        log.info("Nhận yêu cầu POST để từ chối thanh toán COD cho paymentId: {}, lý do: {}, ghi chú: {}",
                paymentId, rejectionRequest.getReason(), rejectionRequest.getNote());
        try {
            PaymentResponseDTO result = paymentService.handleCodRejection(
                    paymentId, rejectionRequest.getReason(), rejectionRequest.getNote());
            log.info("Đã từ chối thành công thanh toán COD cho paymentId: {}", paymentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý từ chối COD cho paymentId {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Thử lại giao hàng COD sau khi bị từ chối
     */
    @PostMapping("/{paymentId}/reattempt-cod")
    public ResponseEntity<PaymentResponseDTO> reattemptCodDelivery(@PathVariable Long paymentId) {
        log.info("Nhận yêu cầu POST để thử lại giao hàng COD cho paymentId: {}", paymentId);
        try {
            PaymentResponseDTO result = paymentService.reattemptCodDelivery(paymentId);
            log.info("Đã yêu cầu thử lại giao hàng COD thành công cho paymentId: {}", paymentId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi thử lại giao hàng COD cho paymentId {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Gửi OTP để xác nhận giao hàng
     */
    @PostMapping("/order/{orderId}/send-otp")
    public ResponseEntity<ApiResponse> sendDeliveryConfirmationOtp(@PathVariable Long orderId) {
        log.info("Nhận yêu cầu POST để gửi OTP xác nhận giao hàng cho orderId: {}", orderId);
        try {
            String result = paymentService.sendDeliveryConfirmationOtp(orderId);
            log.info("Đã gửi OTP thành công cho orderId: {}, kết quả: {}", orderId, result);
            return ResponseEntity.ok(new ApiResponse(true, result));
        } catch (Exception e) {
            log.error("Lỗi khi gửi OTP xác nhận giao hàng cho orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi gửi OTP: " + e.getMessage()));
        }
    }

    /**
     * Xác nhận giao hàng thành công với OTP
     */
    @PostMapping("/order/{orderId}/confirm-delivery")
    public ResponseEntity<PaymentResponseDTO> confirmDeliveryWithOtp(
            @PathVariable Long orderId,
            @RequestBody OtpConfirmRequest otpRequest) {
        log.info("Nhận yêu cầu POST để xác nhận giao hàng với OTP cho orderId: {}, OTP: {}",
                orderId, otpRequest.getOtp());
        try {
            PaymentResponseDTO result = paymentService.confirmDeliveryWithOtp(orderId, otpRequest.getOtp());
            log.info("Đã xác nhận giao hàng thành công với OTP cho orderId: {}", orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận giao hàng với OTP cho orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Hoàn tất đơn hàng (chuyển từ CONFIRMED sang COMPLETED)
     */
    @PostMapping("/order/{orderId}/complete")
    public ResponseEntity<PaymentResponseDTO> completeOrder(@PathVariable Long orderId) {
        log.info("Nhận yêu cầu POST để hoàn tất đơn hàng cho orderId: {}", orderId);
        try {
            PaymentResponseDTO result = paymentService.completeOrder(orderId);
            log.info("Đã hoàn tất thành công đơn hàng cho orderId: {}", orderId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi khi hoàn tất đơn hàng cho orderId {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán với cổng thanh toán
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<ApiResponse> checkPaymentStatus(@PathVariable String transactionId) {
        log.info("Nhận yêu cầu GET để kiểm tra trạng thái thanh toán cho transactionId: {}", transactionId);
        try {
            String status = paymentService.checkPaymentStatusWithGateway(transactionId);
            log.info("Đã kiểm tra trạng thái thanh toán thành công cho transactionId: {}, trạng thái: {}",
                    transactionId, status);
            return ResponseEntity.ok(new ApiResponse(true, "Trạng thái thanh toán: " + status));
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trạng thái thanh toán cho transactionId {}: {}", transactionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi kiểm tra trạng thái: " + e.getMessage()));
        }
    }

    /**
     * Hủy thanh toán (dành cho admin, không cần kiểm tra userId)
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse> cancelPayment(@PathVariable Long paymentId) {
        log.info("Nhận yêu cầu POST để hủy thanh toán cho paymentId: {}", paymentId);
        try {
            ApiResponse response = paymentService.cancelPayment(paymentId, null);
            log.info("Đã hủy thanh toán thành công cho paymentId: {}", paymentId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Lỗi khi hủy thanh toán cho paymentId {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi hủy thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Xác nhận thanh toán VNPay (callback từ VNPay, không yêu cầu xác thực cho admin)
     */
    @GetMapping("/confirm")
    @PreAuthorize("permitAll()") // Cho phép truy cập không cần xác thực vì đây là callback từ VNPay
    public ResponseEntity<ApiResponse> confirmPayment(@RequestParam Map<String, String> vnpayParams) {
        log.info("Nhận yêu cầu GET callback từ VNPay để xác nhận thanh toán với tham số: {}", vnpayParams);
        try {
            PaymentResponseDTO paymentResponse = paymentService.confirmPayment(vnpayParams);
            log.info("Đã xác nhận thanh toán VNPay thành công với transactionCode: {}",
                    paymentResponse != null ? paymentResponse.getTransactionCode() : "N/A");
            return ResponseEntity.ok(new ApiResponse(true, "Xác nhận thanh toán thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi xác nhận thanh toán VNPay: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xác nhận thanh toán: " + e.getMessage()));
        }
    }
}

// DTO cho request cập nhật trạng thái
class PaymentStatusRequest {
    private Payment.PaymentStatus status;

    public Payment.PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(Payment.PaymentStatus status) {
        this.status = status;
    }
}

// DTO cho xác nhận COD
class CodConfirmRequest {
    private String note;

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

// DTO cho từ chối COD
class CodRejectionRequest {
    private String reason; // CUSTOMER_REJECTED, NOT_AVAILABLE, OTHER
    private String note;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

// DTO cho xác nhận OTP
class OtpConfirmRequest {
    private String otp;

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}