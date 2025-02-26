package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.Orders.*;
import com.example.api_sell_clothes_v1.Entity.*;
import com.example.api_sell_clothes_v1.Exceptions.InvalidOrderStatusException;
import com.example.api_sell_clothes_v1.Exceptions.OrderNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.OrderMapper;
import com.example.api_sell_clothes_v1.Repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartsRepository cartsRepository;
    private final CartItemsRepository cartItemsRepository;
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;

    // Shipping fee constants
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    /**
     * Create a new order from cart items
     */
    @Transactional
    public OrderResponseDTO createOrder(Long userId, CreateOrderDTO createDTO) {
        // Get user and validate
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Get user address
        UserAddress address = addressRepository.findById(createDTO.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found"));

        // Verify address belongs to user
        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        // Get user's cart
        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found"));

        // Get selected items from cart
        List<CartItems> cartItems;
        if (createDTO.getSelectedVariantIds() != null && !createDTO.getSelectedVariantIds().isEmpty()) {
            // Store the items with selected variant IDs
            cartItems = new ArrayList<>();
            for (Long variantId : createDTO.getSelectedVariantIds()) {
                cartItemsRepository.findByCartCartIdAndVariantVariantId(cart.getCartId(), variantId)
                        .ifPresent(cartItems::add);
            }
        } else {
            // Use all selected items
            cartItems = cartItemsRepository.findByCartCartIdAndIsSelectedTrue(cart.getCartId());
        }

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("No items selected for order");
        }

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Calculate order amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // Save order first to get ID
        Order savedOrder = orderRepository.save(order);

        for (CartItems cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();

            // Get current price (consider sale price if available)
            BigDecimal currentPrice = variant.getProduct().getSalePrice() != null &&
                    variant.getProduct().getSalePrice().compareTo(BigDecimal.ZERO) > 0 ?
                    variant.getProduct().getSalePrice() :
                    variant.getProduct().getPrice();

            // Create order item using OrderItemService
            OrderItem orderItem = orderItemService.createOrderItem(
                    savedOrder, variant, cartItem.getQuantity(), currentPrice);
            orderItems.add(orderItem);

            // Update total amount
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        // Calculate shipping fee
        BigDecimal shippingFee = calculateShippingFee(totalAmount);
        savedOrder.setShippingFee(shippingFee);

        // Set total amount (includes shipping fee)
        savedOrder.setTotalAmount(totalAmount.add(shippingFee));

        // Update order
        savedOrder = orderRepository.save(savedOrder);

        // Remove items from cart
        for (CartItems item : cartItems) {
            cartItemsRepository.delete(item);
        }

        log.info("Created order with ID: {} for user ID: {}", savedOrder.getOrderId(), userId);
        return orderMapper.toDto(savedOrder);
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        return orderMapper.toDto(order);
    }

    /**
     * Get order by ID for specific user
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getUserOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Verify order belongs to user
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        return orderMapper.toDto(order);
    }

    /**
     * Get all orders for a user
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getUserOrders(Long userId, Pageable pageable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Get user orders by status
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getUserOrdersByStatus(Long userId, Order.OrderStatus status, Pageable pageable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<Order> orders = orderRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Cancel an order
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long userId, Long orderId, CancelOrderDTO cancelDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Verify order belongs to user
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        // Check if order can be cancelled
        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusException("Order cannot be cancelled in its current status: " + order.getStatus());
        }

        // Update order status
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        // Restore stock quantities using OrderItemService
        orderItemService.restoreInventory(orderId);

        Order savedOrder = orderRepository.save(order);
        log.info("Cancelled order with ID: {}", orderId);

        return orderMapper.toDto(savedOrder);
    }

    /**
     * Update order status (admin only)
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusDTO updateDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Validate status transition
        validateStatusTransition(order.getStatus(), updateDTO.getStatus());

        // If cancelling, restore stock
        if (updateDTO.getStatus() == Order.OrderStatus.CANCELLED) {
            orderItemService.restoreInventory(orderId);
        }

        // Update order status
        order.setStatus(updateDTO.getStatus());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Updated order status to {} for order ID: {}", updateDTO.getStatus(), orderId);

        return orderMapper.toDto(savedOrder);
    }

    /**
     * Get all orders (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Get orders by status (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Search orders (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> searchOrders(String search, Pageable pageable) {
        Page<Order> orders = orderRepository.findBySearchTerm(search, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Get filtered orders (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getFilteredOrders(
            Order.OrderStatus status, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Order> orders = orderRepository.findWithFilters(status, userId, startDate, endDate, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Get order statistics (admin only)
     */
    @Transactional(readOnly = true)
    public OrderStatisticsDTO getOrderStatistics() {
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);
        long processingOrders = orderRepository.countByStatus(Order.OrderStatus.PROCESSING);
        long shippingOrders = orderRepository.countByStatus(Order.OrderStatus.SHIPPING);
        long completedOrders = orderRepository.countByStatus(Order.OrderStatus.COMPLETED);
        long cancelledOrders = orderRepository.countByStatus(Order.OrderStatus.CANCELLED);

        return OrderStatisticsDTO.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .shippingOrders(shippingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .build();
    }

    /**
     * Delete order (admin only, for testing)
     */
    @Transactional
    public ApiResponse deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Delete order (OrderItemService will handle deleting items)
        orderItemService.deleteOrderItems(orderId);
        orderRepository.delete(order);
        log.info("Deleted order with ID: {}", orderId);

        return new ApiResponse(true, "Order successfully deleted");
    }

    /**
     * Get bestselling products
     */
    @Transactional(readOnly = true)
    public List<BestsellingProductDTO> getBestsellingProducts(int limit) {
        // Delegate to OrderItemService
        return orderItemService.getBestsellingProducts(limit);
    }

    // Helper methods
    private BigDecimal calculateShippingFee(BigDecimal orderAmount) {
        // Free shipping for orders above the threshold
        if (orderAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return DEFAULT_SHIPPING_FEE;
    }

    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        // Cannot change to the same status
        if (currentStatus == newStatus) {
            throw new InvalidOrderStatusException("Order is already in " + currentStatus + " status");
        }

        // Define valid transitions
        switch (currentStatus) {
            case PENDING:
                // Pending can transition to Processing, Shipping, or Cancelled
                if (newStatus != Order.OrderStatus.PROCESSING &&
                        newStatus != Order.OrderStatus.SHIPPING &&
                        newStatus != Order.OrderStatus.CANCELLED) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case PROCESSING:
                // Processing can transition to Shipping or Cancelled
                if (newStatus != Order.OrderStatus.SHIPPING &&
                        newStatus != Order.OrderStatus.CANCELLED) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case SHIPPING:
                // Shipping can transition to Completed or Cancelled
                if (newStatus != Order.OrderStatus.COMPLETED &&
                        newStatus != Order.OrderStatus.CANCELLED) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case COMPLETED:
                // Completed is a final state and cannot be changed
                throw new InvalidOrderStatusException("Cannot change order status once Completed");

            case CANCELLED:
                // Cancelled is a final state and cannot be changed
                throw new InvalidOrderStatusException("Cannot change order status once Cancelled");

            default:
                throw new InvalidOrderStatusException("Unknown order status: " + currentStatus);
        }
    }
}