package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Orders.*;
import com.example.api_sell_clothes_v1.DTO.Shipping.ApplyShippingDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_ORDERS)
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * Get order by ID (admin)
     */
    @GetMapping("/admin/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * Get all orders (admin)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getAllOrders(
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    /**
     * Get orders by status (admin)
     */
    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getOrdersByStatus(
            @PathVariable Order.OrderStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
    }

    /**
     * Get orders by shipping method (admin)
     */
    @GetMapping("/admin/shipping-method/{methodId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getOrdersByShippingMethod(
            @PathVariable Long methodId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByShippingMethod(methodId, pageable));
    }

    /**
     * Search orders (admin)
     */
    @GetMapping("/admin/search")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> searchOrders(
            @RequestParam String search,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.searchOrders(search, pageable));
    }

    /**
     * Get filtered orders (admin)
     */
    @GetMapping("/admin/filter")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getFilteredOrders(
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getFilteredOrders(status, userId, startDate, endDate, pageable));
    }

    /**
     * Update order status (admin)
     */
    @PutMapping("/admin/{orderId}/status")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusDTO updateDTO) {
        try {
            OrderResponseDTO updatedOrder = orderService.updateOrderStatus(orderId, updateDTO);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Error updating order status: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Update shipping method for an order (admin)
     */
    @PutMapping("/admin/{orderId}/shipping")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<OrderResponseDTO> updateOrderShipping(
            @PathVariable Long orderId,
            @Valid @RequestBody ApplyShippingDTO applyDTO) {
        try {
            OrderResponseDTO updatedOrder = orderService.updateOrderShipping(
                    orderId,
                    applyDTO.getShippingMethodId(),
                    applyDTO.getTotalWeight());
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            log.error("Error updating order shipping: {}", e.getMessage());
            throw new IllegalArgumentException("Lỗi khi cập nhật phương thức vận chuyển: " + e.getMessage());
        }
    }

    /**
     * Get order statistics (admin)
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<OrderStatisticsDTO> getOrderStatistics() {
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

    /**
     * Delete order (admin only, for testing)
     */
    @DeleteMapping("/admin/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.deleteOrder(orderId));
    }
}