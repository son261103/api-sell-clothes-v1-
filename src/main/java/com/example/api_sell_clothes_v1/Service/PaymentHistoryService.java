package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.Payment.PaymentHistoryDTO;
import com.example.api_sell_clothes_v1.Entity.Payment;
import com.example.api_sell_clothes_v1.Entity.PaymentHistory;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.PaymentHistoryMapper;
import com.example.api_sell_clothes_v1.Repository.PaymentHistoryRepository;
import com.example.api_sell_clothes_v1.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryMapper paymentHistoryMapper;

    @Transactional(readOnly = true)
    public List<PaymentHistoryDTO> getPaymentHistoryByPaymentId(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        // Kiểm tra quyền sở hữu nếu là user (không áp dụng cho admin, userId = null)
        if (userId != null && !payment.getOrder().getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền xem lịch sử thanh toán này");
        }

        List<PaymentHistory> histories = paymentHistoryRepository.findByPayment(payment);
        return paymentHistoryMapper.toDto(histories);
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryDTO> getPaymentHistoryByOrderId(Long orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));

        // Kiểm tra quyền sở hữu nếu là user (không áp dụng cho admin, userId = null)
        if (userId != null && !payment.getOrder().getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Bạn không có quyền xem lịch sử thanh toán này");
        }

        List<PaymentHistory> histories = paymentHistoryRepository.findByPayment(payment);
        return paymentHistoryMapper.toDto(histories);
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistoryDTO> getAllPaymentHistory(Pageable pageable) {
        Page<PaymentHistory> histories = paymentHistoryRepository.findAll(pageable);
        return histories.map(paymentHistoryMapper::toDto);
    }

    @Transactional
    public PaymentHistoryDTO addPaymentHistory(Long paymentId, PaymentHistoryDTO dto) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        PaymentHistory history = paymentHistoryMapper.toEntity(dto);
        history.setPayment(payment);
        PaymentHistory savedHistory = paymentHistoryRepository.save(history);

        payment.addPaymentHistory(savedHistory);
        paymentRepository.save(payment);

        return paymentHistoryMapper.toDto(savedHistory);
    }

    @Transactional(readOnly = true)
    public PaymentHistoryDTO getPaymentHistoryById(Long historyId) {
        PaymentHistory history = paymentHistoryRepository.findById(historyId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentHistory", "id", historyId));
        return paymentHistoryMapper.toDto(history);
    }
}