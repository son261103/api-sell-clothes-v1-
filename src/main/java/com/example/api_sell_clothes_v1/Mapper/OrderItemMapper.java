package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Orders.OrderItemDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.OrderItem;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderItemMapper implements EntityMapper<OrderItem, OrderItemDTO> {

    @Override
    public OrderItem toEntity(OrderItemDTO dto) {
        // Need order and variant objects to create an OrderItem
        // so this is typically not used directly - we'll create a helper method for this purpose
        throw new UnsupportedOperationException("Direct conversion from DTO to OrderItem is not supported");
    }

    /**
     * Helper method to create an OrderItem from order, variant, quantity and price
     */
    public OrderItem createOrderItem(Order order, ProductVariant variant, int quantity, BigDecimal unitPrice) {

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(orderItem.getOrderItemId());
        orderItem.setOrder(order);
        orderItem.setVariant(variant);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(unitPrice.multiply(new BigDecimal(quantity)));

        return orderItem;
    }

    @Override
    public OrderItemDTO toDto(OrderItem entity) {
        if (entity == null) {
            return null;
        }

        return OrderItemDTO.builder()
                .orderId(entity.getOrder().getOrderId())
                .variantId(entity.getVariant().getVariantId())
                .productId(entity.getVariant().getProduct().getProductId())
                .productName(entity.getVariant().getProduct().getName())
                .productImage(entity.getVariant().getImageUrl() != null ?
                        entity.getVariant().getImageUrl() :
                        entity.getVariant().getProduct().getThumbnail())
                .sku(entity.getVariant().getSku())
                .size(entity.getVariant().getSize())
                .color(entity.getVariant().getColor())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .totalPrice(entity.getTotalPrice())
                .build();
    }

    @Override
    public List<OrderItem> toEntity(List<OrderItemDTO> dtoList) {
        throw new UnsupportedOperationException("Converting OrderItemDTO list to OrderItem entities is not supported");
    }

    @Override
    public List<OrderItemDTO> toDto(List<OrderItem> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}