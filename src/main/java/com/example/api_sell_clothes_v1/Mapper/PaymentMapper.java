package com.example.api_sell_clothes_v1.Mapper;


import com.example.api_sell_clothes_v1.DTO.Payment.PaymentHistoryDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentMapper implements EntityMapper<Payment, PaymentResponseDTO> {

    private final PaymentHistoryMapper paymentHistoryMapper;

    @Override
    public Payment toEntity(PaymentResponseDTO dto) {
        throw new UnsupportedOperationException("Converting PaymentResponseDTO to Payment entity is not supported");
    }

    @Override
    public PaymentResponseDTO toDto(Payment entity) {
        if (entity == null) {
            return null;
        }

        List<PaymentHistoryDTO> historyDtos = paymentHistoryMapper.toDto(entity.getPaymentHistories());

        return PaymentResponseDTO.builder()
                .paymentId(entity.getPaymentId())
                .orderId(entity.getOrder() != null ? entity.getOrder().getOrderId() : null)
                .paymentMethodName(entity.getPaymentMethod() != null ? entity.getPaymentMethod().getName() : null)
                .paymentMethodCode(entity.getPaymentMethod() != null ? entity.getPaymentMethod().getCode() : null)
                .amount(entity.getAmount())
                .transactionCode(entity.getTransactionCode())
                .paymentStatus(entity.getPaymentStatus().toString())
                .paymentUrl(entity.getPaymentUrl()) // Đã ánh xạ đúng từ entity
                .histories(historyDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<Payment> toEntity(List<PaymentResponseDTO> dtoList) {
        throw new UnsupportedOperationException("Converting PaymentResponseDTO list to Payment entities is not supported");
    }

    @Override
    public List<PaymentResponseDTO> toDto(List<Payment> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}