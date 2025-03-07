package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.*;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Mapper.OrderMapper;
import com.example.api_sell_clothes_v1.Service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_SHIPPING)
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService shippingService;
    private final OrderMapper orderMapper;

    /**
     * USER ENDPOINTS
     */

    /**
     * Get all shipping methods for user
     */
    @GetMapping("/methods")
    public ResponseEntity<List<ShippingMethodDTO>> getAllShippingMethods() {
        return ResponseEntity.ok(shippingService.getAllShippingMethods());
    }

    /**
     * Get shipping method by ID for user
     */
    @GetMapping("/methods/{id}")
    public ResponseEntity<ShippingMethodDTO> getShippingMethodById(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.getShippingMethodById(id));
    }

    /**
     * Estimate shipping cost for cart
     */
    @GetMapping("/estimate")
    @PreAuthorize("hasAuthority('VIEW_CART')")
    public ResponseEntity<ShippingEstimateDTO> estimateShipping(
            @RequestParam BigDecimal orderTotal,
            @RequestParam Long methodId,
            @RequestParam(required = false) Double totalWeight) {
        try {
            return ResponseEntity.ok(shippingService.estimateShipping(orderTotal, methodId, totalWeight));
        } catch (Exception e) {
            log.error("Error estimating shipping: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi ước tính phí vận chuyển: " + e.getMessage());
        }
    }

    /**
     * ADMIN ENDPOINTS
     */

    /**
     * Get all shipping methods (admin)
     */
    @GetMapping("/admin/methods")
    @PreAuthorize("hasAuthority('MANAGE_SHIPPING')")
    public ResponseEntity<List<ShippingMethodDTO>> getAllShippingMethodsAdmin() {
        return ResponseEntity.ok(shippingService.getAllShippingMethods());
    }

    /**
     * Get shipping method by ID (admin)
     */
    @GetMapping("/admin/methods/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SHIPPING')")
    public ResponseEntity<ShippingMethodDTO> getShippingMethodByIdAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(shippingService.getShippingMethodById(id));
    }

    /**
     * Create shipping method (admin only)
     */
    @PostMapping("/admin/methods")
    @PreAuthorize("hasAuthority('MANAGE_SHIPPING')")
    public ResponseEntity<ShippingMethodDTO> createShippingMethod(@Valid @RequestBody ShippingMethodCreateDTO createDTO) {
        try {
            ShippingMethodDTO createdMethod = shippingService.createShippingMethod(createDTO);
            return new ResponseEntity<>(createdMethod, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating shipping method: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi tạo phương thức vận chuyển: " + e.getMessage());
        }
    }

    /**
     * Update shipping method (admin only)
     */
    @PutMapping("/admin/methods/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SHIPPING')")
    public ResponseEntity<ShippingMethodDTO> updateShippingMethod(
            @PathVariable Long id,
            @Valid @RequestBody ShippingMethodUpdateDTO updateDTO) {
        try {
            ShippingMethodDTO updatedMethod = shippingService.updateShippingMethod(id, updateDTO);
            return ResponseEntity.ok(updatedMethod);
        } catch (Exception e) {
            log.error("Error updating shipping method: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật phương thức vận chuyển: " + e.getMessage());
        }
    }

    /**
     * Delete shipping method (admin only)
     */
    @DeleteMapping("/admin/methods/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SHIPPING')")
    public ResponseEntity<ApiResponse> deleteShippingMethod(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(shippingService.deleteShippingMethod(id));
        } catch (Exception e) {
            log.error("Error deleting shipping method: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi xóa phương thức vận chuyển: " + e.getMessage());
        }
    }

    /**
     * Apply shipping to order (admin only)
     */
    @PostMapping("/admin/apply")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<OrderResponseDTO> applyShippingToOrder(@Valid @RequestBody ApplyShippingDTO applyDTO) {
        try {
            Order updatedOrder = shippingService.applyShippingToOrder(
                    applyDTO.getOrderId(),
                    applyDTO.getShippingMethodId(),
                    applyDTO.getTotalWeight()
            );
            return ResponseEntity.ok(orderMapper.toDto(updatedOrder));
        } catch (Exception e) {
            log.error("Error applying shipping to order: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi áp dụng phí vận chuyển cho đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Get shipping fee calculations (admin only)
     */
    @GetMapping("/admin/calculate")
    @PreAuthorize("hasAuthority('MANAGE_SHIPPING')")
    public ResponseEntity<ShippingEstimateDTO> calculateShippingAdmin(
            @RequestParam BigDecimal orderTotal,
            @RequestParam Long methodId,
            @RequestParam(required = false) Double totalWeight) {
        try {
            return ResponseEntity.ok(shippingService.estimateShipping(orderTotal, methodId, totalWeight));
        } catch (Exception e) {
            log.error("Error calculating shipping: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi tính phí vận chuyển: " + e.getMessage());
        }
    }
}