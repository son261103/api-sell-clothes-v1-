package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Carts.CartItemDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartResponseDTO;
import com.example.api_sell_clothes_v1.Entity.CartItems;
import com.example.api_sell_clothes_v1.Entity.Carts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CartMapper implements EntityMapper<Carts, CartResponseDTO> {

    @Override
    public Carts toEntity(CartResponseDTO dto) {
        if (dto == null) {
            return null;
        }

        return Carts.builder()
                .cartId(dto.getCartId())
                .sessionId(dto.getSessionId())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    @Override
    public CartResponseDTO toDto(Carts entity) {
        if (entity == null) {
            return null;
        }

        // Xử lý an toàn trường hợp cartItems là null
        List<CartItemDTO> cartItemDTOs = (entity.getCartItems() != null)
                ? entity.getCartItems().stream()
                .filter(item -> item != null) // Lọc bỏ các item null
                .map(this::toCartItemDto)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return CartResponseDTO.builder()
                .cartId(entity.getCartId())
                .userId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .sessionId(entity.getSessionId())
                .items(cartItemDTOs)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<Carts> toEntity(List<CartResponseDTO> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CartResponseDTO> toDto(List<Carts> entityList) {
        if (entityList == null) {
            return Collections.emptyList();
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển đổi từ CartItems entity sang CartItemDTO
     */
    public CartItemDTO toCartItemDto(CartItems cartItem) {
        if (cartItem == null || cartItem.getVariant() == null || cartItem.getVariant().getProduct() == null) {
            return null; // Trả về null nếu dữ liệu không hợp lệ
        }

        BigDecimal price = cartItem.getVariant().getProduct().getSalePrice() != null
                ? cartItem.getVariant().getProduct().getSalePrice()
                : cartItem.getVariant().getProduct().getPrice();

        BigDecimal totalPrice = (price != null)
                ? price.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                : BigDecimal.ZERO;

        return CartItemDTO.builder()
                .itemId(cartItem.getItemId())
                .cartId(cartItem.getCart() != null ? cartItem.getCart().getCartId() : null)
                .variantId(cartItem.getVariant().getVariantId())
                .productId(cartItem.getVariant().getProduct().getProductId())
                .productName(cartItem.getVariant().getProduct().getName())
                .sku(cartItem.getVariant().getSku())
                .size(cartItem.getVariant().getSize())
                .color(cartItem.getVariant().getColor())
                .quantity(cartItem.getQuantity())
                .unitPrice(price)
                .totalPrice(totalPrice)
                .imageUrl(cartItem.getVariant().getImageUrl() != null
                        ? cartItem.getVariant().getImageUrl()
                        : cartItem.getVariant().getProduct().getThumbnail())
                .stockQuantity(cartItem.getVariant().getStockQuantity())
                .isSelected(cartItem.getIsSelected())
                .build();
    }
}