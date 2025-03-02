package com.example.api_sell_clothes_v1.Mapper;

import com.example.api_sell_clothes_v1.DTO.Carts.CartAddItemDTO;
import com.example.api_sell_clothes_v1.DTO.Carts.CartItemDTO;
import com.example.api_sell_clothes_v1.Entity.CartItems;
import com.example.api_sell_clothes_v1.Entity.Carts;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CartItemMapper implements EntityMapper<CartItems, CartItemDTO> {

    @Override
    public CartItems toEntity(CartItemDTO dto) {
        if (dto == null) {
            return null;
        }

        // Chú ý: Khi chuyển từ DTO sang Entity, cần có thực thể Carts và ProductVariant
        // Thông thường sẽ được xử lý trong service với các repository tương ứng
        return CartItems.builder()
                .itemId(dto.getItemId())
                .quantity(dto.getQuantity())
                .isSelected(dto.getIsSelected())
                .build();
    }

    @Override
    public CartItemDTO toDto(CartItems entity) {
        if (entity == null) {
            return null;
        }

        BigDecimal price = entity.getVariant().getProduct().getSalePrice() != null ?
                entity.getVariant().getProduct().getSalePrice() : entity.getVariant().getProduct().getPrice();

        BigDecimal totalPrice = price != null ?
                price.multiply(BigDecimal.valueOf(entity.getQuantity())) : BigDecimal.ZERO;

        return CartItemDTO.builder()
                .itemId(entity.getItemId())
                .cartId(entity.getCart().getCartId())
                .variantId(entity.getVariant().getVariantId())
                .productId(entity.getVariant().getProduct().getProductId())
                .productName(entity.getVariant().getProduct().getName())
                .sku(entity.getVariant().getSku())
                .size(entity.getVariant().getSize())
                .color(entity.getVariant().getColor())
                .quantity(entity.getQuantity())
                .unitPrice(price)
                .totalPrice(totalPrice)
                .imageUrl(entity.getVariant().getImageUrl() != null ?
                        entity.getVariant().getImageUrl() : entity.getVariant().getProduct().getThumbnail())
                .stockQuantity(entity.getVariant().getStockQuantity())
                .isSelected(entity.getIsSelected())
                .build();
    }

    @Override
    public List<CartItems> toEntity(List<CartItemDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        return dtoList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CartItemDTO> toDto(List<CartItems> entityList) {
        if (entityList == null) {
            return null;
        }
        return entityList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Tạo CartItems từ CartAddItemDTO, Carts và ProductVariant
     */
    public CartItems createFromAddItemDTO(CartAddItemDTO dto, Carts cart, ProductVariant variant) {
        if (dto == null || cart == null || variant == null) {
            return null;
        }

        return CartItems.builder()
                .cart(cart)
                .variant(variant)
                .quantity(dto.getQuantity())
                .isSelected(true) // Mặc định là đã chọn khi thêm vào giỏ hàng
                .build();
    }

    /**
     * Cập nhật số lượng của CartItems hiện có
     */
    public void updateQuantity(CartItems cartItem, Integer quantity) {
        if (cartItem != null && quantity != null && quantity > 0) {
            cartItem.setQuantity(quantity);
        }
    }

    /**
     * Cập nhật trạng thái chọn của CartItems
     */
    public void updateSelection(CartItems cartItem, Boolean isSelected) {
        if (cartItem != null && isSelected != null) {
            cartItem.setIsSelected(isSelected);
        }
    }
}