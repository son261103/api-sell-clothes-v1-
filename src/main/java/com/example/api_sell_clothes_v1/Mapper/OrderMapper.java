package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Orders.OrderItemDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Orders.OrderSummaryDTO;
import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.OrderItem;
import com.example.api_sell_clothes_v1.Entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderMapper implements EntityMapper<Order, OrderResponseDTO> {

    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final PaymentMapper paymentMapper;

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

        // Determine if the order can be cancelled
        boolean canCancel = canCancelOrder(entity);

        return OrderResponseDTO.builder()
                .orderId(entity.getOrderId())
                .user(userDto)
                .address(addressDto)
                .totalAmount(entity.getTotalAmount())
                .shippingFee(entity.getShippingFee())
                .status(entity.getStatus())
                .statusDescription(entity.getStatus().getDescription())
                .orderItems(orderItemDtos)
                .payment(paymentDto) // Ánh xạ thông tin thanh toán
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .canCancel(canCancel)
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

        return OrderSummaryDTO.builder()
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .statusDescription(entity.getStatus().getDescription())
                .totalAmount(entity.getTotalAmount())
                .totalItems(totalItems)
                .createdAt(entity.getCreatedAt())
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