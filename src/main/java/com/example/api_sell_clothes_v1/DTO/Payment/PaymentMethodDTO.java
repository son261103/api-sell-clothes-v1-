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
public class PaymentMethodDTO {
    private Long methodId;
    private String name;
    private String code;
    private String description;
    private Boolean status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}