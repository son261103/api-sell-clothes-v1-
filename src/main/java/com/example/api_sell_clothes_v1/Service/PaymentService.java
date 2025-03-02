package com.example.api_sell_clothes_v1.Service;

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
import com.example.api_sell_clothes_v1.Repository.PaymentMethodRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentMapper paymentMapper;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.payment-url}")
    private String vnpPaymentUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO requestDTO) {
        Order order = orderRepository.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", requestDTO.getOrderId()));

        PaymentMethod paymentMethod = paymentMethodRepository.findById(requestDTO.getMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", requestDTO.getMethodId()));

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .amount(requestDTO.getAmount())
                .paymentStatus(Payment.PaymentStatus.PENDING)
                .build();

        // Nếu là VNPay, tạo URL thanh toán
        if ("vnpay".equalsIgnoreCase(paymentMethod.getCode())) {
            String paymentUrl = createVnpayPaymentUrl(order, requestDTO.getAmount(), requestDTO.getBankCode());
            payment.setPaymentUrl(paymentUrl);
        }

        // Thêm lịch sử thanh toán
        PaymentHistory history = PaymentHistory.builder()
                .payment(payment)
                .status(PaymentHistoryStatus.PENDING)
                .note("Khởi tạo giao dịch " + paymentMethod.getName())
                .build();
        payment.addPaymentHistory(history);

        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);
        orderRepository.save(order);

        return paymentMapper.toDto(savedPayment);
    }

    @Transactional
    public PaymentResponseDTO confirmPayment(Map<String, String> vnpayParams) {
        Long orderId = Long.parseLong(vnpayParams.get("vnp_TxnRef"));
        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // Kiểm tra chữ ký VNPay
        String vnpSecureHash = vnpayParams.get("vnp_SecureHash");
        String calculatedHash = calculateVnpayHash(vnpayParams);
        if (!vnpSecureHash.equals(calculatedHash)) {
            throw new IllegalStateException("Chữ ký VNPay không hợp lệ");
        }

        // Xử lý trạng thái giao dịch
        String responseCode = vnpayParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            payment.setTransactionCode(vnpayParams.get("vnp_TransactionNo"));
            payment.setPaymentStatus(Payment.PaymentStatus.COMPLETED);
            payment.getOrder().setStatus(Order.OrderStatus.CONFIRMED);

            PaymentHistory history = PaymentHistory.builder()
                    .payment(payment)
                    .status(PaymentHistoryStatus.COMPLETED)
                    .note("Giao dịch VNPay hoàn tất - Mã giao dịch: " + vnpayParams.get("vnp_TransactionNo"))
                    .build();
            payment.addPaymentHistory(history);
        } else {
            payment.setPaymentStatus(Payment.PaymentStatus.FAILED);

            PaymentHistory history = PaymentHistory.builder()
                    .payment(payment)
                    .status(PaymentHistoryStatus.FAILED)
                    .note("Giao dịch VNPay thất bại - Mã lỗi: " + responseCode)
                    .build();
            payment.addPaymentHistory(history);
        }

        Payment updatedPayment = paymentRepository.save(payment);
        orderRepository.save(payment.getOrder());

        return paymentMapper.toDto(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentByOrderId(Long orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // Kiểm tra quyền sở hữu nếu là user (không áp dụng cho admin, userId = null)
        if (userId != null && !payment.getOrder().getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền xem thanh toán này");
        }

        return paymentMapper.toDto(payment);
    }

    private String createVnpayPaymentUrl(Order order, BigDecimal amount, String bankCode) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
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

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cal.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cal.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cal.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                        .append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String vnpSecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return vnpPaymentUrl + "?" + query.toString();
    }

    private String calculateVnpayHash(Map<String, String> vnpParams) {
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();

        for (String fieldName : fieldNames) {
            if (!fieldName.equals("vnp_SecureHash")) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(fieldValue);
                    if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1 && !fieldNames.get(fieldNames.size() - 1).equals("vnp_SecureHash")) {
                        hashData.append('&');
                    }
                }
            }
        }

        return hmacSHA512(vnpHashSecret, hashData.toString());
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký VNPay", e);
        }
    }

    private String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}