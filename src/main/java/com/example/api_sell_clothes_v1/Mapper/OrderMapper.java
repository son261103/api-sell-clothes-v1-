package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Coupons.CouponDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderItemDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderSummaryDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.OrderCoupon;
import com.example.api_sell_clothes_v1.Entity.OrderItem;
import com.example.api_sell_clothes_v1.Entity.Payment;
import com.example.api_sell_clothes_v1.Repository.OrderCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper implements EntityMapper<Order, OrderResponseDTO> {

    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final PaymentMapper paymentMapper;
    private final ShippingMapper shippingMapper;
    private final CouponMapper couponMapper;
    private final OrderCouponRepository orderCouponRepository;

    @Override
    public Order toEntity(OrderResponseDTO dto) {
        throw new UnsupportedOperationException("Converting OrderResponseDTO to Order entity is not supported");
    }

    @Override
    public OrderResponseDTO toDto(Order entity) {
        if (entity == null) {
            return null;
        }

        // Map user if available
        UserResponseDTO userDto = entity.getUser() != null ?
                userMapper.toDto(entity.getUser()) : null;

        // Map address if available
        AddressResponseDTO addressDto = entity.getAddress() != null ?
                addressMapper.toDto(entity.getAddress()) : null;

        // Map order items
        List<OrderItemDTO> orderItemDtos = mapOrderItems(entity.getOrderItems());

        // Map payment if available
        PaymentResponseDTO paymentDto = entity.getPayment() != null ?
                paymentMapper.toDto(entity.getPayment()) : null;

        // Map shipping method if available
        ShippingMethodDTO shippingMethodDto = entity.getShippingMethod() != null ?
                shippingMapper.toDto(entity.getShippingMethod()) : null;

        // Determine if the order can be cancelled
        boolean canCancel = canCancelOrder(entity);

        // Map coupons and discount information
        List<CouponDTO> coupons = new ArrayList<>();
        BigDecimal subtotalBeforeDiscount = entity.getTotalAmount();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        List<OrderCoupon> orderCoupons = orderCouponRepository.findByOrderOrderId(entity.getOrderId());
        if (orderCoupons != null && !orderCoupons.isEmpty()) {
            coupons = orderCoupons.stream()
                    .map(oc -> new CouponDTO(
                            oc.getCoupon().getCode(),
                            oc.getCoupon().getType(),
                            oc.getDiscountAmount()))
                    .collect(Collectors.toList());

            // Calculate total discount amount
            totalDiscount = orderCoupons.stream()
                    .map(OrderCoupon::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Calculate original subtotal before discount
            subtotalBeforeDiscount = entity.getTotalAmount().add(totalDiscount)
                    .subtract(entity.getShippingFee() != null ? entity.getShippingFee() : BigDecimal.ZERO);
        } else {
            // If no coupons, subtotal is just total minus shipping
            subtotalBeforeDiscount = entity.getTotalAmount()
                    .subtract(entity.getShippingFee() != null ? entity.getShippingFee() : BigDecimal.ZERO);
        }

        return OrderResponseDTO.builder()
                .orderId(entity.getOrderId())
                .user(userDto)
                .address(addressDto)
                .totalAmount(entity.getTotalAmount())
                .shippingFee(entity.getShippingFee())
                .shippingMethod(shippingMethodDto)
                .status(entity.getStatus())
                .statusDescription(entity.getStatus().getDescription())
                .orderItems(orderItemDtos)
                .payment(paymentDto)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .canCancel(canCancel)
                .coupons(coupons)
                .subtotalBeforeDiscount(subtotalBeforeDiscount)
                .totalDiscount(totalDiscount)
                .build();
    }

    @Override
    public List<Order> toEntity(List<OrderResponseDTO> dtoList) {
        throw new UnsupportedOperationException("Converting OrderResponseDTO list to Order entities is not supported");
    }

    @Override
    public List<OrderResponseDTO> toDto(List<Order> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert an Order entity to an OrderSummaryDTO
     */
    public OrderSummaryDTO toSummaryDto(Order entity) {
        if (entity == null) {
            return null;
        }

        // Count total items
        int totalItems = entity.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        // Get shipping method name if available
        String shippingMethodName = entity.getShippingMethod() != null ?
                entity.getShippingMethod().getName() : null;

        // Generate order code if not present
        String orderCode = "ORD-" + entity.getOrderId();

        // Get payment status
        Payment.PaymentStatus paymentStatus = null;
        if (entity.getPayment() != null) {
            paymentStatus = entity.getPayment().getPaymentStatus();
        }

        // Get user information
        String userName = null;
        String userEmail = null;
        Long userId = null;
        if (entity.getUser() != null) {
            userName = entity.getUser().getFullName();
            userEmail = entity.getUser().getEmail();
            userId = entity.getUser().getUserId();
        }

        // Get discount information
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<OrderCoupon> orderCoupons = orderCouponRepository.findByOrderOrderId(entity.getOrderId());
        if (orderCoupons != null && !orderCoupons.isEmpty()) {
            totalDiscount = orderCoupons.stream()
                    .map(OrderCoupon::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calculate subtotal before discount
        BigDecimal subtotalBeforeDiscount = entity.getTotalAmount().add(totalDiscount)
                .subtract(entity.getShippingFee() != null ? entity.getShippingFee() : BigDecimal.ZERO);

        return OrderSummaryDTO.builder()
                .orderId(entity.getOrderId())
                .orderCode(orderCode)
                .status(entity.getStatus())
                .statusDescription(entity.getStatus().getDescription())
                .totalAmount(entity.getTotalAmount())
                .finalAmount(entity.getTotalAmount()) // Đồng bộ với frontend
                .subtotalBeforeDiscount(subtotalBeforeDiscount)
                .totalDiscount(totalDiscount)
                .shippingFee(entity.getShippingFee())
                .shippingMethodName(shippingMethodName)
                .totalItems(totalItems)
                .itemCount(totalItems) // Đồng bộ với frontend
                .createdAt(entity.getCreatedAt())
                .userName(userName)
                .userEmail(userEmail)
                .userId(userId)
                .paymentStatus(paymentStatus)
                .hasCoupon(!orderCoupons.isEmpty())
                .build();
    }

    /**
     * Convert a list of Order entities to a list of OrderSummaryDTOs
     */
    public List<OrderSummaryDTO> toSummaryDto(List<Order> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Map order items to DTOs
     */
    private List<OrderItemDTO> mapOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return new ArrayList<>();
        }

        return orderItems.stream()
                .map(item -> OrderItemDTO.builder()
                        .orderId(item.getOrder().getOrderId())
                        .variantId(item.getVariant().getVariantId())
                        .productId(item.getVariant().getProduct().getProductId())
                        .productName(item.getVariant().getProduct().getName())
                        .productImage(item.getVariant().getImageUrl() != null ?
                                item.getVariant().getImageUrl() :
                                item.getVariant().getProduct().getThumbnail())
                        .sku(item.getVariant().getSku())
                        .size(item.getVariant().getSize())
                        .color(item.getVariant().getColor())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Determine if the order can be cancelled
     * Orders can only be cancelled if they are in PENDING or CONFIRMED status
     */
    private boolean canCancelOrder(Order order) {
        return order.getStatus() == Order.OrderStatus.PENDING ||
                order.getStatus() == Order.OrderStatus.CONFIRMED;
    }
}