package com.example.api_sell_clothes_v1.DTO.Orders;

import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private Long orderId;
    private String orderCode; // Thêm mã đơn hàng
    private BigDecimal totalAmount;
    private BigDecimal finalAmount; // Đổi tên từ totalAmount để frontend dễ truy cập
    private BigDecimal shippingFee;
    private String shippingMethodName;
    private Order.OrderStatus status;
    private String statusDescription;
    private Payment.PaymentStatus paymentStatus; // Thêm trạng thái thanh toán
    private LocalDateTime createdAt;
    private int totalItems;
    private int itemCount; // Đổi tên từ totalItems để frontend dễ truy cập
    private String userName; // Thêm tên người dùng
    private String userEmail; // Thêm email người dùng
    private Long userId; // Thêm ID người dùng
}