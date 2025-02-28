package com.example.api_sell_clothes_v1.Controller.User;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/address")
@RequiredArgsConstructor
public class UserAddressController {
    private final UserAddressService addressService;

    /**
     * Lấy tất cả địa chỉ của người dùng
     */
    @GetMapping
    public ResponseEntity<?> getUserAddresses(@RequestHeader(value = "X-User-Id") Long userId) {
        try {
            List<AddressResponseDTO> addresses = addressService.getUserAddresses(userId);
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách địa chỉ cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy danh sách địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Lấy địa chỉ theo ID
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<?> getUserAddressById(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long addressId) {
        try {
            AddressResponseDTO address = addressService.getUserAddressById(userId, addressId);
            return ResponseEntity.ok(address);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy địa chỉ: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy địa chỉ {} cho userId {}: {}", addressId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy thông tin địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Thêm địa chỉ mới cho người dùng
     */
    @PostMapping
    public ResponseEntity<?> createAddress(
            @RequestHeader(value = "X-User-Id") Long userId,
            @Valid @RequestBody AddressRequestDTO requestDTO) {
        try {
            AddressResponseDTO createdAddress = addressService.createAddress(userId, requestDTO);
            return new ResponseEntity<>(createdAddress, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi tạo địa chỉ: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi tạo địa chỉ mới cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi tạo địa chỉ mới: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật địa chỉ
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<?> updateAddress(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressDTO updateDTO) {
        try {
            AddressResponseDTO updatedAddress = addressService.updateAddress(userId, addressId, updateDTO);
            return ResponseEntity.ok(updatedAddress);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi cập nhật địa chỉ: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật địa chỉ {} cho userId {}: {}", addressId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi cập nhật địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Xóa địa chỉ
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> deleteAddress(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long addressId) {
        try {
            ApiResponse response = addressService.deleteAddress(userId, addressId);
            return response.isSuccess()
                    ? ResponseEntity.ok(response)
                    : ResponseEntity.badRequest().body(response);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi xóa địa chỉ: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi xóa địa chỉ {} cho userId {}: {}", addressId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi xóa địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Đặt địa chỉ làm mặc định
     */
    @PutMapping("/{addressId}/default")
    public ResponseEntity<?> setDefaultAddress(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long addressId) {
        try {
            AddressResponseDTO address = addressService.setDefaultAddress(userId, addressId);
            return ResponseEntity.ok(address);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi đặt địa chỉ mặc định: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi đặt địa chỉ {} làm mặc định cho userId {}: {}", addressId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi đặt địa chỉ mặc định: " + e.getMessage()));
        }
    }

    /**
     * Lấy địa chỉ mặc định
     */
    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAddress(@RequestHeader(value = "X-User-Id") Long userId) {
        try {
            AddressResponseDTO address = addressService.getDefaultAddress(userId);
            return ResponseEntity.ok(address);
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi lấy địa chỉ mặc định: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi lấy địa chỉ mặc định cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi lấy địa chỉ mặc định: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra số lượng địa chỉ
     */
    @GetMapping("/count")
    public ResponseEntity<?> getAddressCount(@RequestHeader(value = "X-User-Id") Long userId) {
        try {
            Long count = addressService.getAddressCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Lỗi khi đếm số lượng địa chỉ cho userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi đếm số lượng địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra địa chỉ có hợp lệ cho đơn hàng không
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateAddressForOrder(
            @RequestHeader(value = "X-User-Id") Long userId,
            @RequestParam Long addressId) {
        try {
            addressService.validateAddressForOrder(userId, addressId);
            return ResponseEntity.ok(new ApiResponse(true, "Địa chỉ hợp lệ cho đơn hàng"));
        } catch (IllegalArgumentException e) {
            log.error("Dữ liệu không hợp lệ khi kiểm tra địa chỉ cho đơn hàng: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra địa chỉ {} cho đơn hàng của userId {}: {}",
                    addressId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi kiểm tra địa chỉ: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra xem địa chỉ có thuộc về người dùng không
     */
    @GetMapping("/check/{addressId}")
    public ResponseEntity<?> checkAddressBelongsToUser(
            @RequestHeader(value = "X-User-Id") Long userId,
            @PathVariable Long addressId) {
        try {
            boolean belongsToUser = addressService.isAddressBelongToUser(userId, addressId);
            return ResponseEntity.ok(new ApiResponse(belongsToUser,
                    belongsToUser ? "Địa chỉ thuộc về người dùng" : "Địa chỉ không thuộc về người dùng"));
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra quyền sở hữu địa chỉ {} cho userId {}: {}",
                    addressId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Lỗi khi kiểm tra quyền sở hữu địa chỉ: " + e.getMessage()));
        }
    }
}