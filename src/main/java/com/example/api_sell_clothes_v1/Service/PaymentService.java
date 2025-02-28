package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.Config.VnpayConfig;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentRequestDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.Payment;
import com.example.api_sell_clothes_v1.Entity.PaymentHistory;
import com.example.api_sell_clothes_v1.Entity.PaymentMethod;
import com.example.api_sell_clothes_v1.Enums.Status.PaymentHistoryStatus;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.PaymentMapper;
import com.example.api_sell_clothes_v1.Repository.OrderRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentHistoryRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentMethodRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentMapper paymentMapper;
    private final VnpayConfig vnpayConfig;
    private final EmailService emailService;
    private final ObjectMapper objectMapper; // Thêm ObjectMapper

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    /**
     * Lấy tất cả các thanh toán
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getAllPayments() {
        List<Payment> payments = paymentRepository.findAll();
        return payments.stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin thanh toán theo transactionCode
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByTransactionCode(String transactionCode) {
        Payment payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", transactionCode));
        return paymentMapper.toDto(payment);
    }

    /**
     * Lấy danh sách các thanh toán PENDING quá hạn
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPendingPaymentsOlderThan(LocalDateTime threshold) {
        List<Payment> pendingPayments = paymentRepository.findPendingPaymentsOlderThan(
                threshold, Payment.PaymentStatus.PENDING);
        return pendingPayments.stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật trạng thái thanh toán thủ công
     */
    @Transactional
    public PaymentResponseDTO updatePaymentStatus(Long paymentId, Payment.PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        payment.setPaymentStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(status == Payment.PaymentStatus.COMPLETED ? PaymentHistoryStatus.COMPLETED : PaymentHistoryStatus.FAILED)
                .note("Cập nhật trạng thái thủ công bởi admin")
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Admin đã cập nhật trạng thái thanh toán ID: {} thành {}", paymentId, status);
        return paymentMapper.toDto(savedPayment);
    }


    /**
     * Tạo thanh toán mới cho đơn hàng
     */
    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO requestDTO) {
        // Kiểm tra đơn hàng tồn tại
        Order order = orderRepository.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", requestDTO.getOrderId()));

        // Kiểm tra đơn hàng thuộc về người dùng nếu được cung cấp
        Long userId = requestDTO.getUserId();
        if (userId != null && !order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Đơn hàng không thuộc về người dùng này");
        }

        // Kiểm tra đơn hàng đã có thanh toán chưa
        if (order.getPayment() != null && order.getPayment().getPaymentStatus() != Payment.PaymentStatus.FAILED) {
            throw new IllegalArgumentException("Đơn hàng này đã có giao dịch thanh toán");
        }

        // Kiểm tra phương thức thanh toán tồn tại
        PaymentMethod paymentMethod = paymentMethodRepository.findById(requestDTO.getMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", requestDTO.getMethodId()));

        if (!paymentMethod.getStatus()) {
            throw new IllegalArgumentException("Phương thức thanh toán không khả dụng");
        }

        // Tạo thanh toán mới
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .amount(requestDTO.getAmount())
                .paymentStatus(Payment.PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Xử lý theo từng phương thức thanh toán
        if ("VNPAY".equalsIgnoreCase(paymentMethod.getCode())) {
            // Tạo URL thanh toán VNPay
            String paymentUrl = createVnpayPaymentUrl(order, requestDTO.getAmount(), requestDTO.getBankCode());
            payment.setPaymentUrl(paymentUrl);
        } else if ("COD".equalsIgnoreCase(paymentMethod.getCode())) {
            // Thanh toán khi nhận hàng, không cần URL thanh toán
            payment.setPaymentData("Thanh toán khi nhận hàng");
        } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod.getCode())) {
            // Chuyển khoản ngân hàng, cung cấp thông tin chuyển khoản
            String transferInfo = createBankTransferInfo(order);
            payment.setPaymentData(transferInfo);
        }

        // Tạo lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.PENDING)
                .note("Khởi tạo giao dịch " + paymentMethod.getName())
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        // Lưu thanh toán và cập nhật đơn hàng
        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);
        orderRepository.save(order);

        log.info("Đã tạo thanh toán ID: {} cho đơn hàng ID: {}", savedPayment.getPaymentId(), order.getOrderId());
        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Xác nhận thanh toán từ VNPay callback
     */
    @Transactional
    public PaymentResponseDTO confirmPayment(Map<String, String> vnpayParams) {
        log.info("Nhận callback từ VNPay: {}", vnpayParams);

        // Xác thực thông tin từ VNPay
        boolean isValidPayment = vnpayConfig.validatePaymentResponse(vnpayParams);
        if (!isValidPayment) {
            log.error("Thông tin thanh toán từ VNPay không hợp lệ");
            throw new IllegalArgumentException("Thông tin thanh toán từ VNPay không hợp lệ");
        }

        // Lấy thông tin từ VNPay
        Long orderId = Long.parseLong(vnpayParams.get("vnp_TxnRef"));
        String vnpayTransactionId = vnpayParams.get("vnp_TransactionNo");
        String responseCode = vnpayParams.get("vnp_ResponseCode");

        // Tìm đơn hàng và thanh toán
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Payment payment = order.getPayment();
        if (payment == null) {
            log.error("Không tìm thấy thanh toán cho đơn hàng ID: {}", orderId);
            throw new ResourceNotFoundException("Payment", "orderId", orderId);
        }

        // Ghi lại thông tin giao dịch từ VNPay
        payment.setTransactionCode(vnpayTransactionId);
        try {
            payment.setPaymentData(objectMapper.writeValueAsString(vnpayParams));
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi dữ liệu VNPay: {}", e.getMessage());
            payment.setPaymentData("Error serializing VNPay data: " + e.getMessage());
        }
        payment.setUpdatedAt(LocalDateTime.now());

        // Xử lý theo mã phản hồi
        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            return handleSuccessfulPayment(payment, "Thanh toán VNPay thành công - Mã giao dịch: " + vnpayTransactionId);
        } else {
            // Thanh toán thất bại
            return handleFailedPayment(payment, "Thanh toán VNPay thất bại - Mã lỗi: " + responseCode);
        }
    }

    /**
     * Xử lý thanh toán thành công và cập nhật trạng thái đơn hàng
     */
    @Transactional
    public PaymentResponseDTO handleSuccessfulPayment(Payment payment, String note) {
        // Cập nhật trạng thái thanh toán
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());

        // Tạo lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.COMPLETED)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        // Cập nhật đơn hàng
        Order order = payment.getOrder();
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            // Nếu bạn có trường paid trong Order, hãy cập nhật ở đây
            // order.setPaid(true);
            order.setUpdatedAt(LocalDateTime.now());
        }

        // Lưu các thay đổi
        Payment savedPayment = paymentRepository.save(payment);
        orderRepository.save(order);

        // Gửi email thông báo
        sendPaymentConfirmationEmail(savedPayment);

        log.info("Thanh toán ID: {} đã hoàn tất thành công cho đơn hàng ID: {}",
                payment.getPaymentId(), order.getOrderId());

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Xử lý thanh toán thất bại
     */
    @Transactional
    public PaymentResponseDTO handleFailedPayment(Payment payment, String note) {
        // Cập nhật trạng thái thanh toán
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        // Tạo lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.FAILED)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        // Lưu thay đổi
        Payment savedPayment = paymentRepository.save(payment);

        log.warn("Thanh toán ID: {} thất bại cho đơn hàng ID: {}, lý do: {}",
                payment.getPaymentId(), payment.getOrder().getOrderId(), note);

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Lấy thông tin thanh toán theo ID đơn hàng
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByOrderId(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Kiểm tra quyền sở hữu nếu là user (không áp dụng cho admin, userId = null)
        if (userId != null && !order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem thanh toán này");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new ResourceNotFoundException("Payment", "orderId", orderId);
        }

        return paymentMapper.toDto(payment);
    }

    /**
     * Hủy thanh toán
     */
    @Transactional
    public ApiResponse cancelPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        // Kiểm tra quyền hủy thanh toán (nếu userId được cung cấp)
        if (userId != null && !payment.getOrder().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy thanh toán này");
        }

        // Chỉ có thể hủy thanh toán đang chờ xử lý
        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Không thể hủy thanh toán với trạng thái: " + payment.getPaymentStatus());
        }

        // Cập nhật trạng thái thanh toán
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        // Tạo lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.FAILED)
                .note("Thanh toán bị hủy bởi người dùng")
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        paymentRepository.save(payment);

        log.info("Thanh toán ID: {} đã bị hủy", paymentId);
        return new ApiResponse(true, "Hủy thanh toán thành công");
    }

    /**
     * Kiểm tra trạng thái thanh toán với cổng thanh toán
     */
    public String checkPaymentStatusWithGateway(String transactionId) {
        // Tìm đơn hàng bằng transactionId
        Payment payment = paymentRepository.findByTransactionCode(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", transactionId));

        // Xác định cổng thanh toán để gọi API phù hợp
        if ("VNPAY".equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
            // Gọi API VNPay để kiểm tra trạng thái
            // Trong thực tế, bạn sẽ cần gọi API của VNPay tại đây

            // Giả lập kết quả để demo
            return payment.getPaymentStatus().name().toLowerCase();
        }

        // Mặc định trả về trạng thái hiện tại
        return payment.getPaymentStatus().name().toLowerCase();
    }

    /**
     * Xử lý webhook khi thanh toán thành công
     */
    @Transactional
    public void handleWebhookPaymentSuccess(String transactionId) {
        Payment payment = paymentRepository.findByTransactionCode(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", transactionId));

        // Chỉ cập nhật nếu đang ở trạng thái PENDING
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PENDING) {
            handleSuccessfulPayment(payment, "Thanh toán thành công (từ webhook) - Mã giao dịch: " + transactionId);
        }
    }

    /**
     * Xử lý webhook khi thanh toán thất bại
     */
    @Transactional
    public void handleWebhookPaymentFailure(String transactionId, String reason) {
        Payment payment = paymentRepository.findByTransactionCode(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", transactionId));

        // Chỉ cập nhật nếu đang ở trạng thái PENDING
        if (payment.getPaymentStatus() == Payment.PaymentStatus.PENDING) {
            handleFailedPayment(payment, "Thanh toán thất bại (từ webhook) - Lý do: " + reason);
        }
    }

    /**
     * Job định kỳ kiểm tra các thanh toán đang chờ xử lý
     */
    @Scheduled(fixedDelay = 300000) // Chạy mỗi 5 phút
    public void checkPendingPayments() {
        log.info("Đang chạy job kiểm tra các thanh toán đang chờ");

        // Lấy danh sách thanh toán đang chờ xử lý và đã tạo hơn 30 phút
        List<Payment> pendingPayments = paymentRepository.findPendingPaymentsOlderThan(
                LocalDateTime.now().minusMinutes(30),
                Payment.PaymentStatus.PENDING);

        for (Payment payment : pendingPayments) {
            try {
                // Với mỗi phương thức thanh toán, kiểm tra trạng thái thực tế
                if ("VNPAY".equalsIgnoreCase(payment.getPaymentMethod().getCode())
                        && payment.getTransactionCode() != null) {
                    String status = checkPaymentStatusWithGateway(payment.getTransactionCode());

                    if ("completed".equals(status)) {
                        log.info("Tự động cập nhật thanh toán {} thành COMPLETED", payment.getPaymentId());
                        handleSuccessfulPayment(payment, "Thanh toán hoàn tất (tự động kiểm tra)");
                    } else if ("failed".equals(status)) {
                        log.info("Tự động cập nhật thanh toán {} thành FAILED", payment.getPaymentId());
                        handleFailedPayment(payment, "Thanh toán thất bại (tự động kiểm tra)");
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi khi kiểm tra thanh toán ID {}: {}", payment.getPaymentId(), e.getMessage());
            }
        }
    }

    /**
     * Tạo URL thanh toán VNPay
     */
    private String createVnpayPaymentUrl(Order order, BigDecimal amount, String bankCode) {
        Map<String, String> vnpParams = new HashMap<>();

        // Thông tin cơ bản
        vnpParams.put("vnp_Version", vnpayConfig.getVersion());
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal("100")).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", String.valueOf(order.getOrderId()));
        vnpParams.put("vnp_OrderInfo", "Thanh toán đơn hàng #" + order.getOrderId());
        vnpParams.put("vnp_OrderType", "250000");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        if (bankCode != null && !bankCode.isEmpty()) {
            vnpParams.put("vnp_BankCode", bankCode);
        }

        // Tạo URL thanh toán
        return vnpayConfig.createPaymentUrl(vnpParams);
    }

    /**
     * Tạo thông tin chuyển khoản ngân hàng
     */
    private String createBankTransferInfo(Order order) {
        StringBuilder info = new StringBuilder();
        info.append("Ngân hàng: BIDV\n");
        info.append("Số tài khoản: 12345678901234\n");
        info.append("Chủ tài khoản: CÔNG TY TNHH SHOP QUẦN ÁO\n");
        info.append("Số tiền: ").append(order.getTotalAmount()).append(" VND\n");
        info.append("Nội dung: Thanh toan DH ").append(order.getOrderId());

        return info.toString();
    }

    /**
     * Gửi email xác nhận thanh toán
     */
    private void sendPaymentConfirmationEmail(Payment payment) {
        try {
            Order order = payment.getOrder();
            String toEmail = order.getUser().getEmail();
            String customerName = order.getUser().getFullName();
            String orderNumber = order.getOrderId().toString();
            String paymentMethodName = payment.getPaymentMethod().getName();
            String amount = payment.getAmount().toString();
            String transactionId = payment.getTransactionCode() != null ?
                    payment.getTransactionCode() : "N/A";

            emailService.sendPaymentConfirmationEmail(
                    toEmail,
                    customerName,
                    orderNumber,
                    paymentMethodName,
                    amount,
                    transactionId
            );
        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận thanh toán: {}", e.getMessage());
            // Không throw exception ra ngoài vì đây không phải lỗi nghiêm trọng
        }
    }
}