package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderOrderId(Long orderId);

    Optional<Payment> findByTransactionCode(String transactionCode);

    // Sửa từ "Status" thành "PaymentStatus" để khớp với tên thuộc tính trong entity
    boolean existsByOrderOrderIdAndPaymentStatusNot(Long orderId, Payment.PaymentStatus paymentStatus);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status AND p.createdAt < :createdBefore")
    List<Payment> findPendingPaymentsOlderThan(
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("status") Payment.PaymentStatus status);
}