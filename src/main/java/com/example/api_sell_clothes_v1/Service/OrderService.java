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
    private final ShippingRepository shippingMethodRepository;
    private final OrderMapper orderMapper;
    private final OrderItemService orderItemService;
    private final ShippingService shippingService;

    /**
     * Tạo đơn hàng mới từ giỏ hàng
     */
    @Transactional
    public OrderResponseDTO createOrder(Long userId, CreateOrderDTO createDTO) {
        // Kiểm tra người dùng
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Kiểm tra địa chỉ
        UserAddress address = addressRepository.findById(createDTO.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found"));
        if (!address.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        // Lấy giỏ hàng của người dùng
        Carts cart = cartsRepository.findByUserUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found"));

        // Lấy các mục được chọn từ giỏ hàng
        List<CartItems> cartItems = getSelectedCartItems(cart, createDTO.getSelectedVariantIds());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("No items selected for order");
        }

        // Tạo đơn hàng
        Order order = new Order();
        order.setUser(user);
        order.setAddress(address);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Tính tổng tiền hàng (chưa bao gồm phí vận chuyển)
        BigDecimal subTotal = calculateTotalAmount(cartItems);

        // Tính phí vận chuyển
        BigDecimal shippingFee = calculateShippingFee(createDTO, subTotal);
        order.setShippingFee(shippingFee);

        // Tổng tiền cuối cùng bao gồm phí vận chuyển
        BigDecimal totalAmount = subTotal.add(shippingFee);
        order.setTotalAmount(totalAmount);

        // Gán phương thức vận chuyển nếu có
        if (createDTO.getShippingMethodId() != null) {
            ShippingMethod shippingMethod = shippingMethodRepository.findById(createDTO.getShippingMethodId())
                    .orElseThrow(() -> new EntityNotFoundException("Shipping method not found"));
            order.setShippingMethod(shippingMethod);
        } else {
            throw new IllegalArgumentException("Shipping method ID is required");
        }

        // Lưu đơn hàng
        Order savedOrder = orderRepository.save(order);

        // Tạo các mục đơn hàng
        createOrderItems(savedOrder, cartItems);

        log.info("Created order with ID: {} for user ID: {}, shipping_fee: {}, total_amount: {}",
                savedOrder.getOrderId(), userId, shippingFee, totalAmount);
        return orderMapper.toDto(savedOrder);
    }

    /**
     * Lấy thông tin đơn hàng theo ID
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        return orderMapper.toDto(order);
    }

    /**
     * Lấy thông tin đơn hàng của người dùng theo ID
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getUserOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }
        return orderMapper.toDto(order);
    }

    /**
     * Lấy tất cả đơn hàng của người dùng
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getUserOrders(Long userId, Pageable pageable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Page<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Lấy đơn hàng của người dùng theo trạng thái
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getUserOrdersByStatus(Long userId, Order.OrderStatus status, Pageable pageable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Page<Order> orders = orderRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Hủy đơn hàng
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long userId, Long orderId, CancelOrderDTO cancelDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusException("Order cannot be cancelled in its current status: " + order.getStatus());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderItemService.restoreInventory(orderId);

        Order savedOrder = orderRepository.save(order);
        log.info("Cancelled order with ID: {}", orderId);
        return orderMapper.toDto(savedOrder);
    }

    /**
     * Cập nhật trạng thái đơn hàng (admin only)
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusDTO updateDTO) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        validateStatusTransition(order.getStatus(), updateDTO.getStatus());

        if (updateDTO.getStatus() == Order.OrderStatus.CANCELLED) {
            orderItemService.restoreInventory(orderId);
        }

        order.setStatus(updateDTO.getStatus());
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Updated order status to {} for order ID: {}", updateDTO.getStatus(), orderId);
        return orderMapper.toDto(savedOrder);
    }

    /**
     * Cập nhật phương thức vận chuyển (admin only)
     */
    @Transactional
    public OrderResponseDTO updateOrderShipping(Long orderId, Long shippingMethodId, Double totalWeight) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING && order.getStatus() != Order.OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusException("Shipping method can only be updated for orders in PENDING or PROCESSING state");
        }

        ShippingMethod shippingMethod = shippingMethodRepository.findById(shippingMethodId)
                .orElseThrow(() -> new EntityNotFoundException("Shipping method not found"));

        // Tính lại phí vận chuyển dựa trên tổng tiền hàng (loại trừ phí vận chuyển cũ)
        BigDecimal baseAmount = order.getTotalAmount().subtract(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
        BigDecimal newShippingFee = shippingService.calculateShippingFee(baseAmount, shippingMethodId, totalWeight);

        order.setShippingMethod(shippingMethod);
        order.setShippingFee(newShippingFee);
        order.setTotalAmount(baseAmount.add(newShippingFee));
        order.setUpdatedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        log.info("Updated shipping method to {} for order ID: {}, new shipping_fee: {}", shippingMethodId, orderId, newShippingFee);
        return orderMapper.toDto(updatedOrder);
    }

    /**
     * Lấy tất cả đơn hàng (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Lấy đơn hàng theo trạng thái (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Tìm kiếm đơn hàng (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> searchOrders(String search, Pageable pageable) {
        Page<Order> orders = orderRepository.findBySearchTerm(search, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Lấy đơn hàng theo bộ lọc (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getFilteredOrders(
            Order.OrderStatus status, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Order> orders = orderRepository.findWithFilters(status, userId, startDate, endDate, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    /**
     * Lấy thống kê đơn hàng (admin only)
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
     * Xóa đơn hàng (admin only, dùng cho testing)
     */
    @Transactional
    public ApiResponse deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        orderItemService.deleteOrderItems(orderId);
        orderRepository.delete(order);
        log.info("Deleted order with ID: {}", orderId);
        return new ApiResponse(true, "Order successfully deleted");
    }

    /**
     * Lấy danh sách sản phẩm bán chạy
     */
    @Transactional(readOnly = true)
    public List<BestsellingProductDTO> getBestsellingProducts(int limit) {
        return orderItemService.getBestsellingProducts(limit);
    }

    /**
     * Lấy đơn hàng theo phương thức vận chuyển (admin only)
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDTO> getOrdersByShippingMethod(Long shippingMethodId, Pageable pageable) {
        ShippingMethod method = shippingMethodRepository.findById(shippingMethodId)
                .orElseThrow(() -> new EntityNotFoundException("Shipping method not found"));
        Page<Order> orders = orderRepository.findByShippingMethod(method, pageable);
        return orders.map(orderMapper::toSummaryDto);
    }

    // Helper methods
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    private List<CartItems> getSelectedCartItems(Carts cart, List<Long> selectedVariantIds) {
        if (selectedVariantIds != null && !selectedVariantIds.isEmpty()) {
            List<CartItems> items = new ArrayList<>();
            for (Long variantId : selectedVariantIds) {
                cartItemsRepository.findByCartCartIdAndVariantVariantId(cart.getCartId(), variantId)
                        .ifPresent(items::add);
            }
            return items;
        }
        return cartItemsRepository.findByCartCartIdAndIsSelectedTrue(cart.getCartId());
    }

    private BigDecimal calculateTotalAmount(List<CartItems> cartItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItems item : cartItems) {
            ProductVariant variant = item.getVariant();
            BigDecimal price = variant.getProduct().getSalePrice() != null &&
                    variant.getProduct().getSalePrice().compareTo(BigDecimal.ZERO) > 0 ?
                    variant.getProduct().getSalePrice() : variant.getProduct().getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private BigDecimal calculateShippingFee(CreateOrderDTO createDTO, BigDecimal subTotal) {
        if (createDTO.getShippingMethodId() != null) {
            BigDecimal shippingFee = shippingService.calculateShippingFee(subTotal, createDTO.getShippingMethodId(), createDTO.getTotalWeight());
            log.debug("Calculated shipping fee for shipping_method_id {}: {}", createDTO.getShippingMethodId(), shippingFee);
            return shippingFee;
        }
        return calculateDefaultShippingFee(subTotal);
    }

    private BigDecimal calculateDefaultShippingFee(BigDecimal orderAmount) {
        if (orderAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal defaultFee = new BigDecimal("30000");
        log.debug("Applied default shipping fee: {}", defaultFee);
        return defaultFee;
    }

    private List<OrderItem> createOrderItems(Order order, List<CartItems> cartItems) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItems cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();
            BigDecimal price = variant.getProduct().getSalePrice() != null &&
                    variant.getProduct().getSalePrice().compareTo(BigDecimal.ZERO) > 0 ?
                    variant.getProduct().getSalePrice() : variant.getProduct().getPrice();
            OrderItem orderItem = orderItemService.createOrderItem(order, variant, cartItem.getQuantity(), price);
            orderItems.add(orderItem);
        }
        return orderItems;
    }

    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            throw new InvalidOrderStatusException("Order is already in " + currentStatus + " status");
        }

        switch (currentStatus) {
            case PENDING:
                if (newStatus != Order.OrderStatus.PROCESSING &&
                        newStatus != Order.OrderStatus.SHIPPING &&
                        newStatus != Order.OrderStatus.CANCELLED &&
                        newStatus != Order.OrderStatus.CONFIRMED) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case PROCESSING:
                if (newStatus != Order.OrderStatus.SHIPPING && newStatus != Order.OrderStatus.CANCELLED) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case SHIPPING:
                if (newStatus != Order.OrderStatus.COMPLETED && newStatus != Order.OrderStatus.CANCELLED) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case CONFIRMED:
                if (newStatus != Order.OrderStatus.PROCESSING && newStatus != Order.OrderStatus.SHIPPING) {
                    throw new InvalidOrderStatusException("Cannot transition from " + currentStatus + " to " + newStatus);
                }
                break;

            case COMPLETED:
            case CANCELLED:
                throw new InvalidOrderStatusException("Cannot change order status once " + currentStatus);

            default:
                throw new InvalidOrderStatusException("Unknown order status: " + currentStatus);
        }
    }
}