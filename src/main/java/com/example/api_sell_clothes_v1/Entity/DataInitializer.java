package com.example.api_sell_clothes_v1.Entity;

import com.example.api_sell_clothes_v1.Repository.PaymentMethodRepository;
import com.example.api_sell_clothes_v1.Repository.ShippingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private ShippingRepository shippingMethodRepository;

    @PostConstruct
    public void init() {
        // 1. Thêm dữ liệu mặc định cho bảng payment_methods
        if (paymentMethodRepository.count() == 0) {
            // Thanh toán khi nhận hàng (COD)
            PaymentMethod cod = new PaymentMethod();
            cod.setName("Thanh toán khi nhận hàng");
            cod.setCode("COD");
            cod.setDescription("Thanh toán bằng tiền mặt khi nhận hàng");
            cod.setStatus(true);
            paymentMethodRepository.save(cod);

            // Chuyển khoản ngân hàng
            PaymentMethod bankTransfer = new PaymentMethod();
            bankTransfer.setName("Chuyển khoản ngân hàng");
            bankTransfer.setCode("BANK_TRANSFER");
            bankTransfer.setDescription("Thanh toán qua chuyển khoản ngân hàng");
            bankTransfer.setStatus(true);
            paymentMethodRepository.save(bankTransfer);

            // Momo
            PaymentMethod momo = new PaymentMethod();
            momo.setName("Ví Momo");
            momo.setCode("MOMO");
            momo.setDescription("Thanh toán qua ví điện tử Momo");
            momo.setStatus(true);
            paymentMethodRepository.save(momo);

            // VNPay
            PaymentMethod vnPay = new PaymentMethod();
            vnPay.setName("VNPay");
            vnPay.setCode("VNPAY");
            vnPay.setDescription("Thanh toán qua cổng thanh toán VNPay");
            vnPay.setStatus(true);
            paymentMethodRepository.save(vnPay);
        }

        // 2. Thêm dữ liệu mặc định cho bảng shipping_method
        if (shippingMethodRepository.count() == 0) {
            ShippingMethod standard = new ShippingMethod();
            standard.setName("Giao hàng tiêu chuẩn");
            standard.setEstimatedDeliveryTime("3-5 ngày");
            standard.setBaseFee(BigDecimal.valueOf(50000.00));
            standard.setExtraFeePerKg(BigDecimal.valueOf(10000.00));
            shippingMethodRepository.save(standard);

            ShippingMethod express = new ShippingMethod();
            express.setName("Giao hàng nhanh");
            express.setEstimatedDeliveryTime("1-2 ngày");
            express.setBaseFee(BigDecimal.valueOf(100000.00));
            express.setExtraFeePerKg(BigDecimal.valueOf(20000.00));
            shippingMethodRepository.save(express);
        }
    }
}