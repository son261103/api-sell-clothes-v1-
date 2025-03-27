package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.OrderCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderCouponRepository extends JpaRepository<OrderCoupon, Long> {
    List<OrderCoupon> findByOrderOrderId(Long orderId);

    @Query("SELECT SUM(oc.discountAmount) FROM OrderCoupon oc WHERE oc.order.orderId = :orderId")
    BigDecimal getTotalDiscountForOrder(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(oc) FROM OrderCoupon oc WHERE oc.coupon.couponId = :couponId")
    int countUsagesByCouponId(@Param("couponId") Long couponId);

    void deleteByOrderOrderId(Long orderId);
}