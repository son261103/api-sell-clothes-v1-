package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressRequestDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressResponseDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.UpdateAddressDTO;
import com.example.api_sell_clothes_v1.Entity.UserAddress;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.UserAddressMapper;
import com.example.api_sell_clothes_v1.Repository.OrderRepository;
import com.example.api_sell_clothes_v1.Repository.UserAddressRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserAddressMapper addressMapper;

    /**
     * Get all addresses for a user
     */
    @Transactional(readOnly = true)
    public List<AddressResponseDTO> getUserAddresses(Long userId) {
        List<UserAddress> addresses = addressRepository.findByUserUserIdOrderByIsDefaultDesc(userId);
        return addressMapper.toDto(addresses);
    }

    /**
     * Get address by ID
     */
    @Transactional(readOnly = true)
    public AddressResponseDTO getAddressById(Long addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with ID: " + addressId));
        return addressMapper.toDto(address);
    }

    /**
     * Get address by ID for specific user
     */
    @Transactional(readOnly = true)
    public AddressResponseDTO getUserAddressById(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return addressMapper.toDto(address);
    }

    /**
     * Create new address for user
     */
    @Transactional
    public AddressResponseDTO createAddress(Long userId, AddressRequestDTO requestDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // If this is the first address or marked as default, reset other defaults
        if (requestDTO.getIsDefault() != null && requestDTO.getIsDefault() ||
                addressRepository.countByUserUserId(userId) == 0) {
            resetDefaultAddresses(userId);
            requestDTO.setIsDefault(true);
        }

        UserAddress address = addressMapper.createEntity(requestDTO, user);
        UserAddress savedAddress = addressRepository.save(address);

        log.info("Created new address with ID: {} for user ID: {}", savedAddress.getAddressId(), userId);
        return addressMapper.toDto(savedAddress);
    }

    /**
     * Update address
     */
    @Transactional
    public AddressResponseDTO updateAddress(Long userId, Long addressId, UpdateAddressDTO updateDTO) {
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // If setting as default, reset other defaults
        if (updateDTO.getIsDefault() != null && updateDTO.getIsDefault() && !address.getIsDefault()) {
            resetDefaultAddresses(userId);
        }

        // Update address
        addressMapper.updateEntity(address, updateDTO);
        UserAddress savedAddress = addressRepository.save(address);

        log.info("Updated address with ID: {} for user ID: {}", addressId, userId);
        return addressMapper.toDto(savedAddress);
    }

    /**
     * Delete address
     */
    @Transactional
    public ApiResponse deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Check if address is used in any orders
        boolean isUsedInOrders = !orderRepository.findByAddressAddressId(addressId).isEmpty();
        if (isUsedInOrders) {
            return new ApiResponse(false, "Cannot delete address that is used in orders");
        }

        addressRepository.delete(address);

        // If the deleted address was the default, set another one as default
        if (address.getIsDefault()) {
            addressRepository.findFirstByUserUserId(userId).ifPresent(newDefault -> {
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            });
        }

        log.info("Deleted address with ID: {} for user ID: {}", addressId, userId);
        return new ApiResponse(true, "Address deleted successfully");
    }

    /**
     * Set address as default
     */
    @Transactional
    public AddressResponseDTO setDefaultAddress(Long userId, Long addressId) {
        // Verify address exists and belongs to user
        UserAddress address = addressRepository.findByAddressIdAndUserUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Reset all default addresses
        resetDefaultAddresses(userId);

        // Set this address as default
        address.setIsDefault(true);
        UserAddress savedAddress = addressRepository.save(address);

        log.info("Set address ID: {} as default for user ID: {}", addressId, userId);
        return addressMapper.toDto(savedAddress);
    }

    /**
     * Get default address for user
     */
    @Transactional(readOnly = true)
    public AddressResponseDTO getDefaultAddress(Long userId) {
        UserAddress address = addressRepository.findByUserUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> {
                    // If no default address, return the first address or throw exception
                    return addressRepository.findFirstByUserUserId(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("No addresses found for user"));
                });

        return addressMapper.toDto(address);
    }

    /**
     * Get address count for user
     */
    @Transactional(readOnly = true)
    public long getAddressCount(Long userId) {
        return addressRepository.countByUserUserId(userId);
    }

    /**
     * Check if address belongs to user
     */
    @Transactional(readOnly = true)
    public boolean isAddressBelongToUser(Long userId, Long addressId) {
        return addressRepository.findByAddressIdAndUserUserId(addressId, userId).isPresent();
    }

    /**
     * Check if address exists
     */
    @Transactional(readOnly = true)
    public boolean existsAddress(Long addressId) {
        return addressRepository.existsById(addressId);
    }

    /**
     * Helper method to reset default addresses
     */
    private void resetDefaultAddresses(Long userId) {
        List<UserAddress> defaultAddresses = addressRepository.findAllByUserUserIdAndIsDefaultTrue(userId);
        for (UserAddress address : defaultAddresses) {
            address.setIsDefault(false);
        }
        addressRepository.saveAll(defaultAddresses);
    }

    /**
     * Validate address for order
     *
     * @param userId user ID
     * @param addressId address ID
     * @throws ResourceNotFoundException if address doesn't exist
     * @throws IllegalArgumentException if address doesn't belong to user
     */
    @Transactional(readOnly = true)
    public void validateAddressForOrder(Long userId, Long addressId) {
        // Check if address exists
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // Check if address belongs to user
        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }
    }
}