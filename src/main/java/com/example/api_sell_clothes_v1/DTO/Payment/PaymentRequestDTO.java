package com.example.api_sell_clothes_v1.DTO.Payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private Long methodId; // ID của payment_methods (ví dụ: VNPay)

    @NotNull(message = "Số tiền không được để trống")
    private BigDecimal amount;

    @NotNull(message = "ID đơn hàng không được để trống")
    private Long orderId;

    // Optional field for VNPay bank code (e.g., "NCB", "VNPAYQR")
    private String bankCode; // No @NotNull since it's optional
}