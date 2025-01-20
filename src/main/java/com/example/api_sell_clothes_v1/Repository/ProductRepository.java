package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {

    Optional<Products> findBySlug(String slug);

    Optional<Products> findByStatus(Boolean status);

    @Query("SELECT DISTINCT p FROM Products p " +
            "JOIN p.category c " +
            "WHERE c.categoryId = :categoryId " +
            "OR c.parentId = :categoryId")
    List<Products> findByCategoryIdIncludingSubCategories(@Param("categoryId") Long categoryId);

    List<Products> findByBrandBrandId(Long brandId);

    @Query("SELECT p FROM Products p WHERE " +
            "(:categoryId IS NULL OR p.category.categoryId = :categoryId) AND " +
            "(:brandId IS NULL OR p.brand.brandId = :brandId) AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Products> findByFilters(Long categoryId, Long brandId, Boolean status, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Products p WHERE p.status = true")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Products p WHERE p.status = false")
    long countInactiveProducts();

    @Query("SELECT p FROM Products p WHERE " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "(:categoryId IS NULL OR p.category.categoryId = :categoryId) AND " +
            "(:brandId IS NULL OR p.brand.brandId = :brandId) AND " +
            "(:status IS NULL OR p.status = :status)")
    Page<Products> searchProducts(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long categoryId,
            Long brandId,
            Boolean status,
            Pageable pageable);

    Page<Products> findBySalePriceIsNotNullAndStatusIsTrue(Pageable pageable);

    Page<Products> findByStatusIsTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT p FROM Products p WHERE p.status = true " +
            "ORDER BY p.createdAt DESC")
    List<Products> findFeaturedProducts(Pageable pageable);

    default List<Products> findFeaturedProducts(int limit) {
        return findFeaturedProducts(PageRequest.of(0, limit));
    }

    @Query("SELECT p FROM Products p WHERE p.status = true " +
            "AND p.productId <> :productId " +
            "AND (p.category.categoryId = :categoryId OR p.brand.brandId = :brandId) " +
            "ORDER BY " +
            "CASE WHEN p.category.categoryId = :categoryId AND p.brand.brandId = :brandId THEN 1 " +
            "     WHEN p.category.categoryId = :categoryId THEN 2 " +
            "     WHEN p.brand.brandId = :brandId THEN 3 " +
            "     ELSE 4 END, " +
            "p.createdAt DESC")
    List<Products> findRelatedProducts(Long productId, Long categoryId, Long brandId, Pageable pageable);

    default List<Products> findRelatedProducts(Long productId, Long categoryId, Long brandId, int limit) {
        return findRelatedProducts(productId, categoryId, brandId, PageRequest.of(0, limit));
    }
}