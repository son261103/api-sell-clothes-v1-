package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingRepository extends JpaRepository<ShippingMethod, Long> {
    Optional<ShippingMethod> findByName(String name);
}