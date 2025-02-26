package com.example.api_sell_clothes_v1.DTO.Orders;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderDTO {
    @NotBlank(message = "Lý do hủy đơn hàng không được để trống")
    private String cancelReason;
}