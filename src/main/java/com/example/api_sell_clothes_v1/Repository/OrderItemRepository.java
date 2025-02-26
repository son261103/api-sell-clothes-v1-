package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Order;
import com.example.api_sell_clothes_v1.Entity.OrderItem;
import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Find items by order
    List<OrderItem> findByOrder(Order order);

    // Find items by order ID
    List<OrderItem> findByOrderOrderId(Long orderId);

    // Find items by variant
    List<OrderItem> findByVariant(ProductVariant variant);

    // Find items by variant ID
    List<OrderItem> findByVariantVariantId(Long variantId);

    // Find items by product ID
    @Query("SELECT oi FROM OrderItem oi WHERE oi.variant.product.productId = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);

    // Count items in an order
    int countByOrder(Order order);

    // Sum quantity by variant
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.variant.variantId = :variantId")
    Integer sumQuantityByVariantId(@Param("variantId") Long variantId);

    // Find bestselling variants
    @Query("SELECT oi.variant.variantId, SUM(oi.quantity) as total FROM OrderItem oi " +
            "GROUP BY oi.variant.variantId ORDER BY total DESC")
    List<Object[]> findBestSellingVariants(int limit);

    // Find bestselling products
    @Query("SELECT oi.variant.product.productId, SUM(oi.quantity) as total FROM OrderItem oi " +
            "GROUP BY oi.variant.product.productId ORDER BY total DESC")
    List<Object[]> findBestSellingProducts(int limit);

    // Find items by order status
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.status = :status")
    List<OrderItem> findByOrderStatus(@Param("status") Order.OrderStatus status);

    // Delete items by order
    void deleteByOrder(Order order);
}
