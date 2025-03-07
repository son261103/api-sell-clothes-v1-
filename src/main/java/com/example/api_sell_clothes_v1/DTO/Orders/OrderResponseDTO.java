package com.example.api_sell_clothes_v1.DTO.Orders;

import com.example.api_sell_clothes_v1.DTO.Payment.PaymentResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Shipping.ShippingMethodDTO;
import com.example.api_sell_clothes_v1.DTO.UserAddress.AddressResponseDTO;
import com.example.api_sell_clothes_v1.DTO.Users.UserResponseDTO;
import com.example.api_sell_clothes_v1.Entity.Order;
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
public class OrderResponseDTO {
    private Long orderId;
    private UserResponseDTO user;
    private AddressResponseDTO address;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private ShippingMethodDTO shippingMethod;
    private Order.OrderStatus status;
    private String statusDescription;
    private List<OrderItemDTO> orderItems;
    private PaymentResponseDTO payment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canCancel;
}