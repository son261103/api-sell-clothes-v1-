package com.example.api_sell_clothes_v1.Mapper;


import com.example.api_sell_clothes_v1.DTO.Payment.PaymentHistoryDTO;
import com.example.api_sell_clothes_v1.Entity.PaymentHistory;
import com.example.api_sell_clothes_v1.Enums.Status.PaymentHistoryStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentHistoryMapper implements EntityMapper<PaymentHistory, PaymentHistoryDTO> {

    @Override
    public PaymentHistory toEntity(PaymentHistoryDTO dto) {
        if (dto == null) {
            return null;
        }

        return PaymentHistory.builder()
                .historyId(dto.getHistoryId())
                .status(dto.getStatus() != null ? PaymentHistoryStatus.valueOf(dto.getStatus()) : null)
                .note(dto.getNote())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    @Override
    public PaymentHistoryDTO toDto(PaymentHistory entity) {
        if (entity == null) {
            return null;
        }

        return PaymentHistoryDTO.builder()
                .historyId(entity.getHistoryId())
                .paymentId(entity.getPayment() != null ? entity.getPayment().getPaymentId() : null)
                .status(entity.getStatus().toString()) // Chuyển enum thành String
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    @Override
    public List<PaymentHistory> toEntity(List<PaymentHistoryDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentHistoryDTO> toDto(List<PaymentHistory> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}