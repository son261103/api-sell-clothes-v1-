package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Payment.PaymentMethodDTO;
import com.example.api_sell_clothes_v1.Entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMethodMapper implements EntityMapper<PaymentMethod, PaymentMethodDTO> {

    @Override
    public PaymentMethod toEntity(PaymentMethodDTO dto) {
        if (dto == null) {
            return null;
        }

        return PaymentMethod.builder()
                .methodId(dto.getMethodId())
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    @Override
    public PaymentMethodDTO toDto(PaymentMethod entity) {
        if (entity == null) {
            return null;
        }

        return PaymentMethodDTO.builder()
                .methodId(entity.getMethodId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<PaymentMethod> toEntity(List<PaymentMethodDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentMethodDTO> toDto(List<PaymentMethod> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}