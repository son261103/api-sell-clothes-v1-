package com.example.api_sell_clothes_v1.Controller.Admin;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Orders.BestsellingProductDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderItemDTO;
import com.example.api_sell_clothes_v1.Service.OrderItemService;
import com.example.api_sell_clothes_v1.Service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(ApiPatternConstants.API_ORDER_ITEMS)
@RequiredArgsConstructor
public class OrderItemController {
    private final OrderItemService orderItemService;
    private final OrderService orderService;

    /**
     * Get all items in an order
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<List<OrderItemDTO>> getOrderItems(@PathVariable Long orderId, @RequestAttribute("userId") Long userId) {
        // Verify the order belongs to the user
        orderService.getUserOrderById(userId, orderId);
        return ResponseEntity.ok(orderItemService.getOrderItems(orderId));
    }

    /**
     * Get a specific order item
     */
    @GetMapping("/{orderItemId}")
    @PreAuthorize("hasAuthority('VIEW_ORDER')")
    public ResponseEntity<OrderItemDTO> getOrderItem(@PathVariable Long orderItemId) {
        return ResponseEntity.ok(orderItemService.getOrderItem(orderItemId));
    }

    /**
     * ADMIN ENDPOINTS
     */

    /**
     * Get all items in an order (admin)
     */
    @GetMapping("/admin/order/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<List<OrderItemDTO>> getOrderItemsAdmin(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderItemService.getOrderItems(orderId));
    }

    /**
     * Get bestselling variants (admin)
     */
    @GetMapping("/admin/bestselling-variants")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<List<Map<String, Object>>> getBestSellingVariants(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(orderItemService.getBestSellingVariants(limit));
    }

    /**
     * Get bestselling products (admin)
     */
    @GetMapping("/admin/bestselling-products")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<List<BestsellingProductDTO>> getBestsellingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(orderItemService.getBestsellingProducts(limit));
    }

    /**
     * Get sales data by product (admin)
     */
    @GetMapping("/admin/product-sales/{productId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<Map<String, Object>> getProductSalesData(@PathVariable Long productId) {
        return ResponseEntity.ok(orderItemService.getProductSalesData(productId));
    }

    /**
     * Restore inventory for an order (admin)
     */
    @PostMapping("/admin/restore-inventory/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<ApiResponse> restoreInventory(@PathVariable Long orderId) {
        try {
            orderItemService.restoreInventory(orderId);
            return ResponseEntity.ok(new ApiResponse(true, "Inventory has been restored successfully"));
        } catch (Exception e) {
            log.error("Error restoring inventory: {}", e.getMessage());
            return ResponseEntity.ok(new ApiResponse(false, "Error restoring inventory: " + e.getMessage()));
        }
    }

    /**
     * Delete all items for an order (admin use only)
     */
    @DeleteMapping("/admin/order/{orderId}")
    @PreAuthorize("hasAuthority('MANAGE_ORDER')")
    public ResponseEntity<ApiResponse> deleteOrderItems(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderItemService.deleteOrderItems(orderId));
    }
}