package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.ProductReviews;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReviews, Long> {

    Page<ProductReviews> findByProductProductId(Long productId, Pageable pageable);

    Page<ProductReviews> findByUserUserId(Long userId, Pageable pageable);

    Optional<ProductReviews> findByProductProductIdAndUserUserId(Long productId, Long userId);

    // Added method to check if a user has reviewed a product
    boolean existsByProductProductIdAndUserUserId(Long productId, Long userId);

    // Added method to find all reviews by a user for a specific product
    List<ProductReviews> findAllByProductProductIdAndUserUserId(Long productId, Long userId);

    // Added method to find the most recent review by a user for a product
    Optional<ProductReviews> findTopByProductProductIdAndUserUserIdOrderByCreatedAtDesc(Long productId, Long userId);

    @Query("SELECT AVG(pr.rating) FROM ProductReviews pr WHERE pr.product.productId = :productId")
    Double calculateAverageRatingByProductId(@Param("productId") Long productId);

    @Query("SELECT COUNT(pr) FROM ProductReviews pr WHERE pr.product.productId = :productId AND pr.rating = :rating")
    Integer countByProductIdAndRating(@Param("productId") Long productId, @Param("rating") Integer rating);

    @Query("SELECT COUNT(pr) FROM ProductReviews pr WHERE pr.product.productId = :productId")
    Integer countByProductId(@Param("productId") Long productId);

    List<ProductReviews> findTop5ByProductProductIdOrderByCreatedAtDesc(Long productId);
}