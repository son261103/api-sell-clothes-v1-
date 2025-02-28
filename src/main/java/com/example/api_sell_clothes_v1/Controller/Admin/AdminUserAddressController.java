package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressRequestDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressResponseDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.UpdateAddressDTO;
import com.example.api_sell_clothes_v1.Service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_USER_ADDRESSES)
@RequiredArgsConstructor
public class AdminUserAddressController {
    private final UserAddressService addressService;

    /**
     * Get all addresses for the authenticated user
     */
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<List<AddressResponseDTO>> getUserAddresses(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    /**
     * Get address by ID for the authenticated user
     */
    @GetMapping("/{addressId}")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> getUserAddressById(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getUserAddressById(userId, addressId));
    }

    /**
     * Create new address for the authenticated user
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> createAddress(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody AddressRequestDTO requestDTO) {
        try {
            AddressResponseDTO createdAddress = addressService.createAddress(userId, requestDTO);
            return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating address: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi tạo địa chỉ: " + e.getMessage());
        }
    }

    /**
     * Update address for the authenticated user
     */
    @PutMapping("/{addressId}")
    @PreAuthorize("hasAuthority('EDIT_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> updateAddress(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressDTO updateDTO) {
        try {
            AddressResponseDTO updatedAddress = addressService.updateAddress(userId, addressId, updateDTO);
            return ResponseEntity.ok(updatedAddress);
        } catch (Exception e) {
            log.error("Error updating address: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật địa chỉ: " + e.getMessage());
        }
    }

    /**
     * Delete address for the authenticated user
     */
    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAuthority('DELETE_ADDRESS')")
    public ResponseEntity<ApiResponse> deleteAddress(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId) {
        ApiResponse response = addressService.deleteAddress(userId, addressId);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /**
     * Set address as default for the authenticated user
     */
    @PutMapping("/{addressId}/default")
    @PreAuthorize("hasAuthority('SET_DEFAULT_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> setDefaultAddress(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId) {
        try {
            AddressResponseDTO address = addressService.setDefaultAddress(userId, addressId);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            log.error("Error setting default address: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi đặt địa chỉ mặc định: " + e.getMessage());
        }
    }

    /**
     * Get default address for the authenticated user
     */
    @GetMapping("/default")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> getDefaultAddress(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(addressService.getDefaultAddress(userId));
    }

    /**
     * Get address count for the authenticated user
     */
    @GetMapping("/count")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<Long> getAddressCount(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(addressService.getAddressCount(userId));
    }

    /**
     * Check if address exists
     */
    @GetMapping("/check/{addressId}")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<ApiResponse> checkAddressExists(@PathVariable Long addressId) {
        boolean exists = addressService.existsAddress(addressId);
        return ResponseEntity.ok(new ApiResponse(exists, exists ? "Địa chỉ tồn tại" : "Địa chỉ không tồn tại"));
    }

    /**
     * Check if address belongs to user
     */
    @GetMapping("/check/{addressId}/owner")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<ApiResponse> checkAddressBelongsToUser(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId) {
        boolean belongsToUser = addressService.isAddressBelongToUser(userId, addressId);
        return ResponseEntity.ok(new ApiResponse(belongsToUser,
                belongsToUser ? "Địa chỉ thuộc về người dùng" : "Địa chỉ không thuộc về người dùng"));
    }

    /**
     * ADMIN ENDPOINTS
     */

    /**
     * Get address by ID (admin)
     */
    @GetMapping("/admin/{addressId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<AddressResponseDTO> getAddressById(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddressById(addressId));
    }

    /**
     * Get all addresses for a user (admin)
     */
    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<List<AddressResponseDTO>> getAddressesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    /**
     * Create address for a user (admin)
     */
    @PostMapping("/admin/user/{userId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<AddressResponseDTO> createAddressForUser(
            @PathVariable Long userId,
            @Valid @RequestBody AddressRequestDTO requestDTO) {
        try {
            AddressResponseDTO createdAddress = addressService.createAddress(userId, requestDTO);
            return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating address: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi tạo địa chỉ: " + e.getMessage());
        }
    }

    /**
     * Update address (admin)
     */
    @PutMapping("/admin/{addressId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<AddressResponseDTO> updateAddressAdmin(
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressDTO updateDTO) {
        try {
            // Get the address to find the user
            AddressResponseDTO address = addressService.getAddressById(addressId);
            AddressResponseDTO updatedAddress = addressService.updateAddress(address.getUserId(), addressId, updateDTO);
            return ResponseEntity.ok(updatedAddress);
        } catch (Exception e) {
            log.error("Error updating address: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật địa chỉ: " + e.getMessage());
        }
    }

    /**
     * Delete address (admin)
     */
    @DeleteMapping("/admin/{addressId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<ApiResponse> deleteAddressAdmin(@PathVariable Long addressId) {
        try {
            // Get the address to find the user
            AddressResponseDTO address = addressService.getAddressById(addressId);
            ApiResponse response = addressService.deleteAddress(address.getUserId(), addressId);
            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error deleting address: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Lỗi khi xóa địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Validate address for order
     */
    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('VALIDATE_ADDRESS')")
    public ResponseEntity<ApiResponse> validateAddressForOrder(
            @RequestAttribute("userId") Long userId,
            @RequestParam Long addressId) {
        try {
            addressService.validateAddressForOrder(userId, addressId);
            return ResponseEntity.ok(new ApiResponse(true, "Địa chỉ hợp lệ cho đơn hàng"));
        } catch (Exception e) {
            log.error("Error validating address: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }
}