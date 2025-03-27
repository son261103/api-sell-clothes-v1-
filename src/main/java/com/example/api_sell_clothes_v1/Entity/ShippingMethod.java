package com.example.api_sell_clothes_v1.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_method")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "estimated_delivery_time", length = 50)
    private String estimatedDeliveryTime;

    @Column(name = "base_fee", nullable = false)
    private BigDecimal baseFee;

    @Column(name = "extra_fee_per_kg", nullable = true, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal extraFeePerKg;
}