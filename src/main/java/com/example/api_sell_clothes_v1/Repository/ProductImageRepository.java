package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.ProductImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImages, Long> {
    List<ProductImages> findByProductProductIdOrderByDisplayOrderAsc(Long productId);

    Optional<ProductImages> findByProductProductIdAndIsPrimaryTrue(Long productId);

    @Query("SELECT COUNT(pi) FROM ProductImages pi WHERE pi.product.productId = :productId")
    long countByProductId(Long productId);

    @Query("SELECT COUNT(pi) FROM ProductImages pi WHERE pi.product.productId = :productId AND pi.isPrimary = true")
    long countPrimaryImagesByProductId(Long productId);

    @Modifying
    @Query("UPDATE ProductImages pi SET pi.isPrimary = false WHERE pi.product.productId = :productId AND pi.imageId != :excludeImageId")
    void updateOtherImagesNonPrimary(Long productId, Long excludeImageId);

    @Query("SELECT MAX(pi.displayOrder) FROM ProductImages pi WHERE pi.product.productId = :productId")
    Optional<Integer> findMaxDisplayOrderByProductId(Long productId);
}
