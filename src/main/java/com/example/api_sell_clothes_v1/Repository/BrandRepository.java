package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.Brands;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brands, Long> {
    /**
     * Find brand by name (case insensitive)
     */
    Optional<Brands> findByNameIgnoreCase(String name);

    /**
     * Check if brand exists by name (case insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find all active brands
     */
    List<Brands> findByStatusTrue();

    /**
     * Find all inactive brands
     */
    List<Brands> findByStatusFalse();

    /**
     * Search brands by name containing keyword (case insensitive)
     */
    List<Brands> findByNameContainingIgnoreCase(String keyword);

    /**
     * Get brand statistics
     */
    @Query("SELECT COUNT(b), " +
            "SUM(CASE WHEN b.status = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN b.status = false THEN 1 ELSE 0 END) " +
            "FROM Brands b")
    Object[] getBrandStatistics();


    /**
     * Find brands sorted by name
     */
    List<Brands> findAllByOrderByNameAsc();

    boolean existsByBrandIdAndStatus(Long brandId, boolean status);


    Page<Brands> findByStatusAndNameContainingIgnoreCase(Boolean status, String name, Pageable pageable);

    Page<Brands> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Brands> findByStatus(Boolean status, Pageable pageable);
}