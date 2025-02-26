package com.example.api_sell_clothes_v1.DTO.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDTO {
    private Long historyId;
    private Long paymentId;
    private String status; // Giữ String để trả về chuỗi trạng thái
    private String note;
    private LocalDateTime createdAt;
}