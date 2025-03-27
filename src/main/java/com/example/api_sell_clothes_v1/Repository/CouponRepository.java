package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeAndStatusTrue(String code);

    @Query("SELECT c FROM Coupon c WHERE c.status = true AND " +
            "(c.startDate IS NULL OR c.startDate <= CURRENT_TIMESTAMP) AND " +
            "(c.endDate IS NULL OR c.endDate >= CURRENT_TIMESTAMP) AND " +
            "(c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    List<Coupon> findAllValidCoupons();

    @Query("SELECT c FROM Coupon c WHERE " +
            "(:code IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:isExpired IS NULL OR (:isExpired = true AND c.endDate < CURRENT_TIMESTAMP) OR " +
            "(:isExpired = false AND (c.endDate IS NULL OR c.endDate >= CURRENT_TIMESTAMP)))")
    Page<Coupon> searchCoupons(
            @Param("code") String code,
            @Param("status") Boolean status,
            @Param("isExpired") Boolean isExpired,
            Pageable pageable);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.status = true")
    long countActiveCoupons();

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.endDate < CURRENT_TIMESTAMP")
    long countExpiredCoupons();

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.usageLimit IS NOT NULL AND c.usedCount >= c.usageLimit")
    long countFullyUsedCoupons();
}