package com.example.api_sell_clothes_v1.DTO.Orders;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDTO {
    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private Long addressId;

    // Các variantId đã được chọn trong giỏ hàng
    // Nếu trống, lấy tất cả các sản phẩm đã chọn (isSelected=true) trong giỏ hàng
    private List<Long> selectedVariantIds;

    // Ghi chú đơn hàng
    private String notes;

    private Long shippingMethodId;

    private Double totalWeight;
}