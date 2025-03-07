package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByStatus(Boolean status);

    Optional<PaymentMethod> findByCode(String code);

    boolean existsByCode(String code);
}