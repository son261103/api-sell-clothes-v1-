package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressRequestDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressResponseDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.UpdateAddressDTO;
import com.example.api_sell_clothes_v1.Entity.UserAddress;
import com.example.api_sell_clothes_v1.Entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAddressMapper implements EntityMapper<UserAddress, AddressResponseDTO> {

    @Override
    public UserAddress toEntity(AddressResponseDTO dto) {
        throw new UnsupportedOperationException("Converting AddressResponseDTO to UserAddress entity is not supported");
    }

    /**
     * Create a UserAddress entity from AddressRequestDTO and user
     */
    public UserAddress createEntity(AddressRequestDTO dto, Users user) {
        return UserAddress.builder()
                .user(user)
                .addressLine(dto.getAddressLine())
                .city(dto.getCity())
                .district(dto.getDistrict())
                .ward(dto.getWard())
                .phoneNumber(dto.getPhoneNumber())
                .isDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false)
                .build();
    }

    /**
     * Update UserAddress entity from UpdateAddressDTO
     */
    public void updateEntity(UserAddress entity, UpdateAddressDTO dto) {
        entity.setAddressLine(dto.getAddressLine());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setWard(dto.getWard());
        entity.setPhoneNumber(dto.getPhoneNumber());

        // Only update isDefault if explicitly provided
        if (dto.getIsDefault() != null) {
            entity.setIsDefault(dto.getIsDefault());
        }
    }

    @Override
    public AddressResponseDTO toDto(UserAddress entity) {
        if (entity == null) {
            return null;
        }

        // Construct full address string for display
        String fullAddress = buildFullAddress(entity);

        return AddressResponseDTO.builder()
                .addressId(entity.getAddressId())
                .userId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .addressLine(entity.getAddressLine())
                .city(entity.getCity())
                .district(entity.getDistrict())
                .ward(entity.getWard())
                .phoneNumber(entity.getPhoneNumber())
                .isDefault(entity.getIsDefault())
                .fullAddress(fullAddress)
                .build();
    }

    @Override
    public List<UserAddress> toEntity(List<AddressResponseDTO> dtoList) {
        throw new UnsupportedOperationException("Converting AddressResponseDTO list to UserAddress entities is not supported");
    }

    @Override
    public List<AddressResponseDTO> toDto(List<UserAddress> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Build a full address string from address parts
     */
    private String buildFullAddress(UserAddress address) {
        StringBuilder sb = new StringBuilder(address.getAddressLine());

        if (address.getWard() != null && !address.getWard().isEmpty()) {
            sb.append(", ").append(address.getWard());
        }

        if (address.getDistrict() != null && !address.getDistrict().isEmpty()) {
            sb.append(", ").append(address.getDistrict());
        }

        if (address.getCity() != null && !address.getCity().isEmpty()) {
            sb.append(", ").append(address.getCity());
        }

        return sb.toString();
    }
}