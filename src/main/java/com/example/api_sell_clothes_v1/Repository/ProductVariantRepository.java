package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Đếm số biến thể có SKU bắt đầu bằng một chuỗi
     */
    long countBySkuStartingWith(String skuPrefix);

    /**
     * Đếm số lượng tồn kho của tất cả biến thể của một sản phẩm
     */
    Integer countStockQuantityByProductProductId(Long productId);

    /**
     * Tìm biến thể có số lượng tồn kho dưới ngưỡng
     */
    List<ProductVariant> findByStockQuantityLessThanAndStatus(Integer threshold, Boolean status);


    // Basic queries
    List<ProductVariant> findByProductProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    // Count queries
    long countByProductProductIdAndStatusIsTrue(Long productId);

    long countByProductProductIdAndStatusIsFalse(Long productId);

    // Status based queries
    List<ProductVariant> findByProductProductIdAndStatusIsTrue(Long productId);

    List<ProductVariant> findByProductProductIdAndStatus(Long productId, Boolean status);

    // Size and Color queries
    List<ProductVariant> findByProductProductIdAndSize(Long productId, String size);

    List<ProductVariant> findByProductProductIdAndColor(Long productId, String color);

    @Query("SELECT DISTINCT v.size FROM ProductVariant v " +
            "WHERE v.product.productId = :productId AND v.status = true " +
            "ORDER BY v.size")
    List<String> findDistinctSizesByProductId(@Param("productId") Long productId);

    @Query("SELECT DISTINCT v.color FROM ProductVariant v " +
            "WHERE v.product.productId = :productId AND v.status = true " +
            "ORDER BY v.color")
    List<String> findDistinctColorsByProductId(@Param("productId") Long productId);

    // Stock queries
    @Query("SELECT v FROM ProductVariant v " +
            "WHERE v.stockQuantity <= :threshold AND v.status = true")
    List<ProductVariant> findLowStockVariants(@Param("threshold") int threshold);

    @Query("SELECT v FROM ProductVariant v " +
            "WHERE v.stockQuantity = 0 AND v.status = true")
    List<ProductVariant> findOutOfStockVariants();

    // Filtered search
    @Query("SELECT v FROM ProductVariant v WHERE " +
            "(:productId IS NULL OR v.product.productId = :productId) AND " +
            "(:size IS NULL OR v.size = :size) AND " +
            "(:color IS NULL OR v.color = :color) AND " +
            "(:status IS NULL OR v.status = :status)")
    Page<ProductVariant> findByFilters(
            @Param("productId") Long productId,
            @Param("size") String size,
            @Param("color") String color,
            @Param("status") Boolean status,
            Pageable pageable);

    // Available variants
    @Query("SELECT v FROM ProductVariant v " +
            "WHERE v.product.productId = :productId " +
            "AND v.size = :size " +
            "AND v.color = :color " +
            "AND v.status = true " +
            "AND v.stockQuantity > 0")
    Optional<ProductVariant> findAvailableVariant(
            @Param("productId") Long productId,
            @Param("size") String size,
            @Param("color") String color);

    // Check product availability
    @Query("SELECT COUNT(v) > 0 FROM ProductVariant v " +
            "WHERE v.product.productId = :productId " +
            "AND v.stockQuantity > 0 " +
            "AND v.status = true")
    boolean hasAvailableVariants(@Param("productId") Long productId);
}