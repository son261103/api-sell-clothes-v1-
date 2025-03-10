package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Coupon;
import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.ShippingMethod;
import com.example.api_sell_clothes_v1.Entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Find orders by user
    List<Order> findByUserOrderByCreatedAtDesc(Users user);

    // Find orders by user with pagination
    Page<Order> findByUserOrderByCreatedAtDesc(Users user, Pageable pageable);

    // Find orders by status
    List<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status);

    // Find orders by status with pagination
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);

    // Find orders between date range
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find orders by user and status
    List<Order> findByUserAndStatusOrderByCreatedAtDesc(Users user, Order.OrderStatus status);

    // Find orders by user and status with pagination
    Page<Order> findByUserAndStatusOrderByCreatedAtDesc(Users user, Order.OrderStatus status, Pageable pageable);

    // Find recent orders
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);

    // Count orders by status
    Long countByStatus(Order.OrderStatus status);

    // Find orders by user ID
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    // Find orders by user ID with pagination
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.createdAt DESC")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // Find orders with search by order ID or user information
    @Query("SELECT o FROM Order o WHERE CAST(o.orderId AS string) LIKE %:search% OR " +
            "o.user.fullName LIKE %:search% OR o.user.email LIKE %:search% OR " +
            "o.user.phone LIKE %:search% ORDER BY o.createdAt DESC")
    Page<Order> findBySearchTerm(@Param("search") String search, Pageable pageable);

    // Find orders with combined filters
    @Query("SELECT o FROM Order o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:userId IS NULL OR o.user.userId = :userId) AND " +
            "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR o.createdAt <= :endDate) " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findWithFilters(
            @Param("status") Order.OrderStatus status,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Find orders pending for more than specified time
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt <= :thresholdTime")
    List<Order> findPendingOrdersOlderThan(@Param("thresholdTime") LocalDateTime thresholdTime);

    // Find last order by user
    Optional<Order> findFirstByUserOrderByCreatedAtDesc(Users user);

    // Find orders by address ID
    List<Order> findByAddressAddressId(Long addressId);

    // Check if an address is used in any orders
    boolean existsByAddressAddressId(Long addressId);

    @Query("SELECT o FROM Order o JOIN o.orderCoupons oc WHERE oc.coupon = :coupon")
    Page<Order> findByCoupon(@Param("coupon") Coupon coupon, Pageable pageable);


    // Added for shipping method support
    Page<Order> findByShippingMethod(ShippingMethod shippingMethod, Pageable pageable);

}