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
import com.example.api_sell_clothes_v1.Repository.CartsRepository;
import com.example.api_sell_clothes_v1.Repository.CartItemsRepository;
import com.example.api_sell_clothes_v1.Repository.OrderRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentHistoryRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentMethodRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final CartsRepository cartsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final PaymentMapper paymentMapper;
    private final VnpayConfig vnpayConfig;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final OrderItemService orderItemService;
    private final SpringTemplateEngine templateEngine;

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
     * Cập nhật trạng thái thanh toán thủ công (cho admin)
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

        // Kiểm tra quyền sở hữu đơn hàng
        Long userId = requestDTO.getUserId();
        if (userId != null && !order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Đơn hàng không thuộc về người dùng này");
        }

        // Kiểm tra đã có thanh toán chưa
        if (order.getPayment() != null && order.getPayment().getPaymentStatus() != Payment.PaymentStatus.FAILED) {
            throw new IllegalArgumentException("Đơn hàng này đã có giao dịch thanh toán");
        }

        // Kiểm tra phương thức thanh toán
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

        String fullPaymentUrl = null;
        Map<String, String> paymentData = new HashMap<>();

        try {
            if ("VNPAY".equalsIgnoreCase(paymentMethod.getCode())) {
                fullPaymentUrl = createVnpayPaymentUrl(order, requestDTO.getAmount(), requestDTO.getBankCode());
                paymentData.put("method", "VNPAY");
                paymentData.put("description", "Thanh toán qua VNPay");
            } else if ("COD".equalsIgnoreCase(paymentMethod.getCode())) {
                paymentData.put("method", "COD");
                paymentData.put("description", "Thanh toán khi nhận hàng");
                notifyAdminForCodOrder(order); // Gửi thông báo cho admin
            } else if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethod.getCode())) {
                String transferInfo = createBankTransferInfo(order);
                paymentData.put("method", "BANK_TRANSFER");
                paymentData.put("description", transferInfo);
            } else {
                // Xử lý mặc định cho các phương thức khác
                paymentData.put("method", paymentMethod.getCode());
                paymentData.put("description", paymentMethod.getName());
            }

            payment.setPaymentData(objectMapper.writeValueAsString(paymentData));
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi payment_data thành JSON: {}", e.getMessage());
            payment.setPaymentData("{\"error\": \"Failed to serialize payment data\"}");
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

        PaymentResponseDTO responseDTO = paymentMapper.toDto(savedPayment);
        if (fullPaymentUrl != null) {
            responseDTO.setPaymentUrl(fullPaymentUrl);
        }

        log.info("Đã tạo thanh toán ID: {} cho đơn hàng ID: {}", savedPayment.getPaymentId(), order.getOrderId());
        return responseDTO;
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
            throw new ResourceNotFoundException("Payment", "orderId", orderId);
        }

        // Ghi lại thông tin giao dịch
        payment.setTransactionCode(vnpayTransactionId);
        try {
            payment.setPaymentData(objectMapper.writeValueAsString(vnpayParams));
        } catch (Exception e) {
            log.error("Lỗi khi lưu dữ liệu VNPay: {}", e.getMessage());
            payment.setPaymentData("{\"error\": \"Failed to serialize VNPay data\"}");
        }
        payment.setUpdatedAt(LocalDateTime.now());

        if ("00".equals(responseCode)) {
            return handleSuccessfulPayment(payment, "Thanh toán VNPay thành công - Mã giao dịch: " + vnpayTransactionId);
        } else {
            return handleFailedPayment(payment, "Thanh toán VNPay thất bại - Mã lỗi: " + responseCode);
        }
    }

    /**
     * Xác nhận thanh toán COD bởi admin
     */
    @Transactional
    public PaymentResponseDTO confirmCodPayment(Long paymentId, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!"COD".equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận thanh toán COD");
        }

        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Thanh toán này không ở trạng thái PENDING");
        }

        return handleSuccessfulPayment(payment, "Thanh toán COD đã được xác nhận bởi admin - Ghi chú: " + note);
    }

    /**
     * Xử lý thanh toán thành công và cập nhật trạng thái đơn hàng
     */
    @Transactional
    public PaymentResponseDTO handleSuccessfulPayment(Payment payment, String note) {
        payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.COMPLETED)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        Order order = payment.getOrder();
        if (order.getStatus() == Order.OrderStatus.PENDING) {
            // Đối với thanh toán online, chuyển đơn hàng sang PROCESSING
            if (!"COD".equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
                order.setStatus(Order.OrderStatus.PROCESSING);
            }
            // Đối với COD, giữ nguyên trạng thái PENDING vì thanh toán chỉ hoàn tất khi giao hàng

            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Xóa giỏ hàng khi thanh toán thành công
            Long userId = order.getUser().getUserId();
            cartsRepository.findByUserUserId(userId).ifPresent(cart -> {
                List<com.example.api_sell_clothes_v1.Entity.CartItems> cartItems = cartItemsRepository.findByCartCartId(cart.getCartId());
                cartItemsRepository.deleteAll(cartItems);
            });
        }

        Payment savedPayment = paymentRepository.save(payment);
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
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.FAILED)
                .note(note)
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        Payment savedPayment = paymentRepository.save(payment);

        log.warn("Thanh toán ID: {} thất bại cho đơn hàng ID: {}, lý do: {}",
                payment.getPaymentId(), payment.getOrder().getOrderId(), note);
        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Xử lý từ chối giao hàng COD
     */
    @Transactional
    public PaymentResponseDTO handleCodRejection(Long paymentId, String reason, String note) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!"COD".equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
            throw new IllegalArgumentException("Chỉ có thể xử lý từ chối cho thanh toán COD");
        }

        // Cập nhật trạng thái thanh toán
        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

        // Thêm lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.FAILED)
                .note("Giao hàng COD thất bại - Lý do: " + reason + " - Ghi chú: " + note)
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        // Cập nhật trạng thái đơn hàng
        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.DELIVERY_FAILED);  // Sử dụng trạng thái mới DELIVERY_FAILED
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Khôi phục lại tồn kho sản phẩm nếu cần
        if (reason.equals("CUSTOMER_REJECTED") || reason.equals("PERMANENTLY_UNAVAILABLE")) {
            orderItemService.restoreInventory(order.getOrderId());
        }

        // Gửi email thông báo
        sendCodRejectionEmail(payment, reason, note);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Đã xử lý từ chối COD cho thanh toán ID: {}, lý do: {}", paymentId, reason);

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Thử lại giao hàng COD sau khi bị từ chối
     */
    @Transactional
    public PaymentResponseDTO reattemptCodDelivery(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!"COD".equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
            throw new IllegalArgumentException("Chỉ có thể thử lại giao hàng cho thanh toán COD");
        }

        if (payment.getPaymentStatus() != Payment.PaymentStatus.FAILED) {
            throw new IllegalArgumentException("Chỉ có thể thử lại giao hàng cho thanh toán thất bại");
        }

        Order order = payment.getOrder();
        if (order.getStatus() != Order.OrderStatus.DELIVERY_FAILED) {
            throw new IllegalArgumentException("Chỉ có thể thử lại giao hàng cho đơn hàng có trạng thái DELIVERY_FAILED");
        }

        // Đặt lại trạng thái thanh toán
        payment.setPaymentStatus(Payment.PaymentStatus.PENDING);
        payment.setUpdatedAt(LocalDateTime.now());

        // Thêm lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.PENDING)
                .note("Thử lại giao hàng COD")
                .createdAt(LocalDateTime.now())
                .build();
        payment.addPaymentHistory(history);

        // Cập nhật trạng thái đơn hàng
        order.setStatus(Order.OrderStatus.PROCESSING);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Gửi email thông báo
        sendCodReattemptEmail(payment);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Đã đặt lại thanh toán ID: {} để thử lại giao hàng COD", paymentId);

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Gửi OTP để xác nhận nhận hàng COD
     */
    @Transactional
    public String sendDeliveryConfirmationOtp(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() != Order.OrderStatus.SHIPPING) {
            throw new IllegalArgumentException("Chỉ có thể gửi OTP xác nhận cho đơn hàng đang giao");
        }

        // Tạo OTP
        String otp = generateOtp(6);

        // Lưu OTP vào đơn hàng (có thể cần thêm trường để lưu)
        order.setDeliveryOtp(otp);
        order.setDeliveryOtpExpiry(LocalDateTime.now().plusMinutes(15)); // OTP hết hạn sau 15 phút
        orderRepository.save(order);

        // Gửi OTP qua email
        sendDeliveryOtpEmail(order, otp);

        log.info("Đã gửi OTP xác nhận giao hàng cho đơn hàng ID: {}", orderId);
        return "Đã gửi OTP xác nhận giao hàng đến email của khách hàng";
    }

    /**
     * Xác nhận giao hàng thành công với OTP
     */
    @Transactional
    public PaymentResponseDTO confirmDeliveryWithOtp(Long orderId, String otp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() != Order.OrderStatus.SHIPPING) {
            throw new IllegalArgumentException("Chỉ có thể xác nhận giao hàng cho đơn hàng đang giao");
        }

        // Kiểm tra OTP
        if (order.getDeliveryOtp() == null || !order.getDeliveryOtp().equals(otp)) {
            throw new IllegalArgumentException("OTP không hợp lệ");
        }

        // Kiểm tra thời hạn OTP
        if (order.getDeliveryOtpExpiry() == null || LocalDateTime.now().isAfter(order.getDeliveryOtpExpiry())) {
            throw new IllegalArgumentException("OTP đã hết hạn");
        }

        // Xác nhận giao hàng thành công
        order.setStatus(Order.OrderStatus.CONFIRMED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setDeliveryOtp(null); // Xóa OTP sau khi xác nhận
        order.setDeliveryOtpExpiry(null);
        orderRepository.save(order);

        Payment payment = null;
        // Nếu là COD, xác nhận thanh toán
        if (order.getPayment() != null &&
                "COD".equalsIgnoreCase(order.getPayment().getPaymentMethod().getCode()) &&
                order.getPayment().getPaymentStatus() == Payment.PaymentStatus.PENDING) {

            payment = order.getPayment();
            // Cập nhật trạng thái thanh toán
            payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());

            // Thêm lịch sử thanh toán
            PaymentHistory history = PaymentHistory.builder()
                    .payment(payment)
                    .status(PaymentHistoryStatus.COMPLETED)
                    .note("Thanh toán COD đã được xác nhận khi giao hàng với OTP")
                    .createdAt(LocalDateTime.now())
                    .build();
            payment.addPaymentHistory(history);

            payment = paymentRepository.save(payment);

            // Gửi email xác nhận thanh toán
            sendPaymentConfirmationEmail(payment);
        }

        // Gửi email thông báo
        emailService.sendOrderStatusUpdateEmail(
                order.getUser().getEmail(),
                order.getUser().getFullName(),
                order.getOrderId().toString(),
                "Đã giao hàng thành công"
        );

        log.info("Đã xác nhận giao hàng thành công với OTP cho đơn hàng ID: {}", orderId);

        if (payment != null) {
            return paymentMapper.toDto(payment);
        } else if (order.getPayment() != null) {
            return paymentMapper.toDto(order.getPayment());
        } else {
            return null; // hoặc có thể tạo PaymentResponseDTO rỗng hoặc throw exception tùy vào yêu cầu
        }
    }

    /**
     * Hoàn tất đơn hàng (chuyển từ CONFIRMED sang COMPLETED)
     */
    @Transactional
    public PaymentResponseDTO completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() != Order.OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Chỉ có thể hoàn tất đơn hàng đã được xác nhận giao thành công");
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Gửi email thông báo
        emailService.sendOrderStatusUpdateEmail(
                order.getUser().getEmail(),
                order.getUser().getFullName(),
                order.getOrderId().toString(),
                "Đơn hàng đã hoàn tất"
        );

        log.info("Đã hoàn tất đơn hàng ID: {}", orderId);

        // Trả về thông tin thanh toán nếu có
        if (order.getPayment() != null) {
            return paymentMapper.toDto(order.getPayment());
        } else {
            return null; // hoặc có thể tạo PaymentResponseDTO rỗng hoặc throw exception tùy vào yêu cầu
        }
    }

    /**
     * Chuyển hướng đến URL thanh toán (giữ lại nhưng không cần thiết nếu không dùng)
     */
    public ResponseEntity<?> redirectToPayment(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
            Payment payment = order.getPayment();
            if (payment == null || payment.getPaymentData() == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Không tìm thấy thông tin thanh toán"));
            }

            String paymentData = payment.getPaymentData();
            Map<String, String> data = objectMapper.readValue(paymentData, HashMap.class);
            String fullUrl = data.get("fullPaymentUrl");
            if (fullUrl == null || fullUrl.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "URL thanh toán không hợp lệ"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(fullUrl));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển hướng thanh toán: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi chuyển hướng thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Lấy thông tin thanh toán theo ID đơn hàng
     */
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByOrderId(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (userId != null && !order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem thanh toán này");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            // Return null instead of throwing an exception
            return null;
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

        if (userId != null && !payment.getOrder().getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy thanh toán này");
        }

        if (payment.getPaymentStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Không thể hủy thanh toán với trạng thái: " + payment.getPaymentStatus());
        }

        payment.setPaymentStatus(Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());

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
        Payment payment = paymentRepository.findByTransactionCode(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", transactionId));

        if ("VNPAY".equalsIgnoreCase(payment.getPaymentMethod().getCode())) {
            // Gọi API VNPay để kiểm tra trạng thái (giả lập)
            return payment.getPaymentStatus().name().toLowerCase();
        }

        return payment.getPaymentStatus().name().toLowerCase();
    }

    /**
     * Xử lý webhook khi thanh toán thành công
     */
    @Transactional
    public void handleWebhookPaymentSuccess(String transactionId) {
        Payment payment = paymentRepository.findByTransactionCode(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionCode", transactionId));

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

        List<Payment> pendingPayments = paymentRepository.findPendingPaymentsOlderThan(
                LocalDateTime.now().minusMinutes(30),
                Payment.PaymentStatus.PENDING);

        for (Payment payment : pendingPayments) {
            try {
                if ("VNPAY".equalsIgnoreCase(payment.getPaymentMethod().getCode()) && payment.getTransactionCode() != null) {
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
            String transactionId = payment.getTransactionCode() != null ? payment.getTransactionCode() : "N/A";

            emailService.sendPaymentConfirmationEmail(
                    toEmail,
                    customerName,
                    orderNumber,
                    paymentMethodName,
                    amount,
                    transactionId
            );
            log.info("Đã gửi email xác nhận thanh toán cho đơn hàng ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email xác nhận thanh toán cho đơn hàng ID: {}. Chi tiết: {}",
                    payment.getOrder().getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Gửi thông báo cho admin khi đặt hàng COD
     */
    private void notifyAdminForCodOrder(Order order) {
        try {
            String adminEmail = "admin@example.com"; // Thay bằng email admin thực tế từ cấu hình
            String subject = "Thông báo đơn hàng COD mới: #" + order.getOrderId();
            String message = String.format(
                    "Đơn hàng #%d đã được đặt với phương thức COD.\n" +
                            "Tổng tiền: %s VND\n" +
                            "Người đặt: %s\n" +
                            "Vui lòng xác nhận khi nhận được thanh toán.",
                    order.getOrderId(),
                    order.getTotalAmount().toString(),
                    order.getUser().getFullName()
            );
            emailService.sendEmail(adminEmail, subject, message);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo COD cho admin: {}", e.getMessage());
        }
    }

    /**
     * Gửi email thông báo từ chối COD
     */
    private void sendCodRejectionEmail(Payment payment, String reason, String note) {
        try {
            Order order = payment.getOrder();
            String toEmail = order.getUser().getEmail();
            String customerName = order.getUser().getFullName();
            String orderNumber = order.getOrderId().toString();

            // Map mã lý do từ chối sang mô tả dễ hiểu
            String reasonDescription;
            switch(reason) {
                case "CUSTOMER_REJECTED":
                    reasonDescription = "Khách hàng từ chối nhận hàng";
                    break;
                case "NOT_AVAILABLE":
                    reasonDescription = "Khách hàng không có ở nhà/không liên lạc được";
                    break;
                default:
                    reasonDescription = reason;
            }

            // Tạo context cho template email
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("reasonDescription", reasonDescription);
            context.setVariable("note", note);
            context.setVariable("updateDate", LocalDateTime.now().toString());

            String emailContent = templateEngine.process("Mail/codRejectionNotification", context);

            emailService.sendHtmlEmail(toEmail, "Thông báo về đơn hàng #" + orderNumber, emailContent);
            log.info("Đã gửi email thông báo từ chối COD cho đơn hàng ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo từ chối COD: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi email thông báo thử lại giao hàng COD
     */
    private void sendCodReattemptEmail(Payment payment) {
        try {
            Order order = payment.getOrder();
            String toEmail = order.getUser().getEmail();
            String customerName = order.getUser().getFullName();
            String orderNumber = order.getOrderId().toString();

            // Tạo context cho template email
            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("updateDate", LocalDateTime.now().toString());

            String emailContent = templateEngine.process("Mail/codReattemptNotification", context);

            emailService.sendHtmlEmail(toEmail, "Thông báo giao lại đơn hàng #" + orderNumber, emailContent);
            log.info("Đã gửi email thông báo giao lại COD cho đơn hàng ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email thông báo giao lại COD: {}", e.getMessage(), e);
        }
    }

    /**
     * Gửi email OTP xác nhận giao hàng
     */
    private void sendDeliveryOtpEmail(Order order, String otp) {
        try {
            String toEmail = order.getUser().getEmail();
            String customerName = order.getUser().getFullName();
            String orderNumber = order.getOrderId().toString();

            Context context = new Context();
            context.setVariable("customerName", customerName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("otp", otp);
            context.setVariable("expiry", "15 phút");

            String emailContent = templateEngine.process("Mail/deliveryOtpEmail", context);

            emailService.sendHtmlEmail(toEmail, "Mã OTP xác nhận giao hàng - Đơn hàng #" + orderNumber, emailContent);
            log.info("Đã gửi email OTP xác nhận giao hàng cho đơn hàng ID: {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP xác nhận giao hàng: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo mã OTP ngẫu nhiên
     */
    private String generateOtp(int length) {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}