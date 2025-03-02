package com.example.api_sell_clothes_v1.DTO.Payment;

import com.example.api_sell_clothes_v1.DTO.Payment.PaymentHistoryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {
    private Long paymentId;
    private Long orderId;
    private String paymentMethodName;
    private String paymentMethodCode;
    private BigDecimal amount;
    private String transactionCode;
    private String paymentStatus;
    private String paymentUrl;
    private List<PaymentHistoryDTO> histories; // Trường histories
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}