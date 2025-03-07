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
@RequestMapping(ApiPatternConstants.API_USER_ADDRESSES) // /api/v1/user-addresses
@RequiredArgsConstructor
public class AdminUserAddressController {
    private final UserAddressService addressService;

    // Endpoint cho người dùng
    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<List<AddressResponseDTO>> getUserAddresses(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> getUserAddressById(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getUserAddressById(userId, addressId));
    }

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

    @GetMapping("/default")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<AddressResponseDTO> getDefaultAddress(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(addressService.getDefaultAddress(userId));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<Long> getAddressCount(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(addressService.getAddressCount(userId));
    }

    @GetMapping("/check/{addressId}")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<ApiResponse> checkAddressExists(@PathVariable Long addressId) {
        boolean exists = addressService.existsAddress(addressId);
        return ResponseEntity.ok(new ApiResponse(exists, exists ? "Địa chỉ tồn tại" : "Địa chỉ không tồn tại"));
    }

    @GetMapping("/check/{addressId}/owner")
    @PreAuthorize("hasAuthority('VIEW_ADDRESS')")
    public ResponseEntity<ApiResponse> checkAddressBelongsToUser(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long addressId) {
        boolean belongsToUser = addressService.isAddressBelongToUser(userId, addressId);
        return ResponseEntity.ok(new ApiResponse(belongsToUser,
                belongsToUser ? "Địa chỉ thuộc về người dùng" : "Địa chỉ không thuộc về người dùng"));
    }

    // Endpoint cho admin
    @GetMapping("/admin/{addressId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<AddressResponseDTO> getAddressById(@PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.getAddressById(addressId));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<List<AddressResponseDTO>> getAddressesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @PostMapping("/user/{userId}")
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

    @PutMapping("/admin/{addressId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<AddressResponseDTO> updateAddressAdmin(
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressDTO updateDTO) {
        try {
            AddressResponseDTO address = addressService.getAddressById(addressId);
            AddressResponseDTO updatedAddress = addressService.updateAddress(address.getUserId(), addressId, updateDTO);
            return ResponseEntity.ok(updatedAddress);
        } catch (Exception e) {
            log.error("Error updating address: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật địa chỉ: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/{addressId}")
    @PreAuthorize("hasAuthority('MANAGE_CUSTOMER_ADDRESSES')")
    public ResponseEntity<ApiResponse> deleteAddressAdmin(@PathVariable Long addressId) {
        try {
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