package com.example.api_sell_clothes_v1.DTO.Orders;

import com.example.api_sell_clothes_v1.Entity.Order.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusDTO {
    @NotNull(message = "Trạng thái đơn hàng không được để trống")
    private OrderStatus status;

    private String notes;
}