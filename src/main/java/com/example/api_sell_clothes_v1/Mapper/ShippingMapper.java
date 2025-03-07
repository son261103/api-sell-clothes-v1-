package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodCreateDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.ShippingMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ShippingMapper implements EntityMapper<ShippingMethod, ShippingMethodDTO> {

    @Override
    public ShippingMethod toEntity(ShippingMethodDTO dto) {
        if (dto == null) {
            return null;
        }

        return ShippingMethod.builder()
                .id(dto.getId())
                .name(dto.getName())
                .estimatedDeliveryTime(dto.getEstimatedDeliveryTime())
                .baseFee(dto.getBaseFee())
                .extraFeePerKg(dto.getExtraFeePerKg())
                .build();
    }

    @Override
    public ShippingMethodDTO toDto(ShippingMethod entity) {
        if (entity == null) {
            return null;
        }

        // Create ShippingMethodDTO and set properties
        ShippingMethodDTO shippingMethodDTO = new ShippingMethodDTO();
        shippingMethodDTO.setId(entity.getId());
        shippingMethodDTO.setName(entity.getName());
        shippingMethodDTO.setEstimatedDeliveryTime(entity.getEstimatedDeliveryTime());
        shippingMethodDTO.setBaseFee(entity.getBaseFee());
        shippingMethodDTO.setExtraFeePerKg(entity.getExtraFeePerKg());

        return shippingMethodDTO;
    }

    @Override
    public List<ShippingMethod> toEntity(List<ShippingMethodDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShippingMethodDTO> toDto(List<ShippingMethod> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Additional method for CreateDTO
    public ShippingMethod toEntity(ShippingMethodCreateDTO createDTO) {
        if (createDTO == null) {
            return null;
        }

        return ShippingMethod.builder()
                .name(createDTO.getName())
                .estimatedDeliveryTime(createDTO.getEstimatedDeliveryTime())
                .baseFee(createDTO.getBaseFee())
                .extraFeePerKg(createDTO.getExtraFeePerKg())
                .build();
    }

    // Additional method for UpdateDTO
    public void updateEntityFromDTO(ShippingMethodUpdateDTO updateDTO, ShippingMethod shippingMethod) {
        if (updateDTO == null || shippingMethod == null) {
            return;
        }

        if (updateDTO.getName() != null) {
            shippingMethod.setName(updateDTO.getName());
        }
        if (updateDTO.getEstimatedDeliveryTime() != null) {
            shippingMethod.setEstimatedDeliveryTime(updateDTO.getEstimatedDeliveryTime());
        }
        if (updateDTO.getBaseFee() != null) {
            shippingMethod.setBaseFee(updateDTO.getBaseFee());
        }
        if (updateDTO.getExtraFeePerKg() != null) {
            shippingMethod.setExtraFeePerKg(updateDTO.getExtraFeePerKg());
        }
    }
}