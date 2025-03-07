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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_ORDERS)
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    /**
     * Get order by ID (admin)
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * Get all orders (admin)
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getAllOrders(
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable,
            Authentication authentication) {
        log.info("Received request to get all orders with pageable: {}", pageable);
        log.info("Authentication: {}", authentication);
        if (authentication != null) {
            log.info("User: {}", authentication.getName());
            log.info("Authorities: {}", authentication.getAuthorities());
        } else {
            log.info("No authentication found");
        }
        Page<OrderSummaryDTO> orders = orderService.getAllOrders(pageable);
        log.info("Returning {} orders", orders.getTotalElements());
        return ResponseEntity.ok(orders);
    }

    /**
     * Get orders by status (admin)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getOrdersByStatus(
            @PathVariable Order.OrderStatus status,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
    }

    /**
     * Get orders by shipping method (admin)
     */
    @GetMapping("/shipping-method/{methodId}")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> getOrdersByShippingMethod(
            @PathVariable Long methodId,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByShippingMethod(methodId, pageable));
    }

    /**
     * Search orders (admin)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<Page<OrderSummaryDTO>> searchOrders(
            @RequestParam String search,
            @PageableDefault(page = 0, size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(orderService.searchOrders(search, pageable));
    }

    /**
     * Get filtered orders (admin)
     */
    @GetMapping("/filter")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
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
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('EDIT_ORDER')")
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
    @PutMapping("/{orderId}/shipping")
    @PreAuthorize("hasAuthority('EDIT_ORDER')")
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
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<OrderStatisticsDTO> getOrderStatistics() {
        return ResponseEntity.ok(orderService.getOrderStatistics());
    }

    /**
     * Delete order (admin only, for testing)
     */
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAuthority('DELETE_ORDER')")
    public ResponseEntity<ApiResponse> deleteOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.deleteOrder(orderId));
    }
}