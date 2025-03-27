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

    @NotNull(message = "Phương thức vận chuyển không được để trống")
    private Long shippingMethodId;

    private Double totalWeight;

    private List<Long> selectedVariantIds;

    // Thêm mã giảm giá
    private String couponCode;

    // Thêm ghi chú đơn hàng nếu cần
    private String orderNote;
}