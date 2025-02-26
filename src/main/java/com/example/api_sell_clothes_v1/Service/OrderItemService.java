package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Orders.BestsellingProductDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderItemDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.OrderItem;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.OrderItemMapper;
import com.example.api_sell_clothes_v1.Repository.OrderItemRepository;
import com.example.api_sell_clothes_v1.Repository.OrderRepository;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import com.example.api_sell_clothes_v1.Repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemService {
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final OrderItemMapper orderItemMapper;

    /**
     * Get all items in an order
     */
    @Transactional(readOnly = true)
    public List<OrderItemDTO> getOrderItems(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        List<OrderItem> orderItems = orderItemRepository.findByOrder(order);
        return orderItemMapper.toDto(orderItems);
    }

    /**
     * Get a specific order item
     */
    @Transactional(readOnly = true)
    public OrderItemDTO getOrderItem(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with ID: " + orderItemId));

        return orderItemMapper.toDto(orderItem);
    }

    /**
     * Create an order item (typically used internally by OrderService)
     */
    @Transactional
    public OrderItem createOrderItem(Order order, ProductVariant variant, int quantity, BigDecimal unitPrice) {
        // Check stock availability
        if (variant.getStockQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Not enough stock for " + variant.getProduct().getName() +
                            " (" + variant.getSize() + ", " + variant.getColor() + ")");
        }

        // Create order item
        OrderItem orderItem = orderItemMapper.createOrderItem(order, variant, quantity, unitPrice);

        // Update stock quantity
        variant.setStockQuantity(variant.getStockQuantity() - quantity);
        variantRepository.save(variant);

        // Save and return
        return orderItemRepository.save(orderItem);
    }

    /**
     * Restore inventory when order is cancelled
     */
    @Transactional
    public void restoreInventory(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);

        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            variantRepository.save(variant);

            log.info("Restored {} units to variant ID: {}", item.getQuantity(), variant.getVariantId());
        }
    }

    /**
     * Get bestselling variants
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBestSellingVariants(int limit) {
        List<Object[]> results = orderItemRepository.findBestSellingVariants(limit);
        List<Map<String, Object>> variantsList = new ArrayList<>();

        for (Object[] result : results) {
            Long variantId = (Long) result[0];
            Long quantity = (Long) result[1];

            ProductVariant variant = variantRepository.findById(variantId).orElse(null);

            Map<String, Object> variantMap = new HashMap<>();
            if (variant != null) {
                variantMap.put("variantId", variantId);
                variantMap.put("productId", variant.getProduct().getProductId());
                variantMap.put("productName", variant.getProduct().getName());
                variantMap.put("size", variant.getSize());
                variantMap.put("color", variant.getColor());
                variantMap.put("quantitySold", quantity);
            } else {
                variantMap.put("variantId", variantId);
                variantMap.put("quantitySold", quantity);
                variantMap.put("note", "Variant details not available");
            }
            variantsList.add(variantMap);
        }

        return variantsList;
    }

    /**
     * Get bestselling products
     */
    @Transactional(readOnly = true)
    public List<BestsellingProductDTO> getBestsellingProducts(int limit) {
        List<Object[]> results = orderItemRepository.findBestSellingProducts(limit);

        List<BestsellingProductDTO> bestsellingProducts = new ArrayList<>();
        for (Object[] result : results) {
            Long productId = (Long) result[0];
            Long totalQuantity = (Long) result[1];

            productRepository.findById(productId).ifPresent(product -> {
                BestsellingProductDTO dto = BestsellingProductDTO.builder()
                        .productId(productId)
                        .productName(product.getName())
                        .productImage(product.getThumbnail())
                        .totalQuantitySold(totalQuantity.intValue())
                        .build();

                bestsellingProducts.add(dto);
            });
        }

        return bestsellingProducts;
    }

    /**
     * Get sales data by product
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductSalesData(Long productId) {
        List<OrderItem> items = orderItemRepository.findByProductId(productId);

        int totalQuantitySold = items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        BigDecimal totalRevenue = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group sales by variant
        Map<Long, Integer> salesByVariant = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getVariant().getVariantId(),
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        // Convert to more readable format
        List<Map<String, Object>> variantSales = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : salesByVariant.entrySet()) {
            ProductVariant variant = variantRepository.findById(entry.getKey()).orElse(null);

            Map<String, Object> variantMap = new HashMap<>();
            if (variant != null) {
                variantMap.put("variantId", entry.getKey());
                variantMap.put("size", variant.getSize());
                variantMap.put("color", variant.getColor());
                variantMap.put("quantitySold", entry.getValue());
            } else {
                variantMap.put("variantId", entry.getKey());
                variantMap.put("quantitySold", entry.getValue());
                variantMap.put("note", "Variant details not available");
            }
            variantSales.add(variantMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("productId", productId);
        result.put("totalQuantitySold", totalQuantitySold);
        result.put("totalRevenue", totalRevenue);
        result.put("variantSales", variantSales);

        return result;
    }

    /**
     * Delete all items for an order (admin use only)
     */
    @Transactional
    public ApiResponse deleteOrderItems(Long orderId) {
        orderItemRepository.deleteByOrder(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId)));

        return new ApiResponse(true, "Order items deleted successfully");
    }
}