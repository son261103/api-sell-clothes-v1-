package com.example.api_sell_clothes_v1.Repository;

import com.example.api_sell_clothes_v1.Entity.ReviewComments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComments, Long> {

    /**
     * Find comments by review ID with pagination
     */
    Page<ReviewComments> findByReviewReviewId(Long reviewId, Pageable pageable);

    /**
     * Find comments by user ID with pagination
     */
    Page<ReviewComments> findByUserUserId(Long userId, Pageable pageable);

    /**
     * Delete all comments for a review
     */
    @Modifying
    @Query("DELETE FROM ReviewComments c WHERE c.review.reviewId = :reviewId")
    void deleteAllByReviewReviewId(Long reviewId);

    /**
     * Count comments for a review
     */
    long countByReviewReviewId(Long reviewId);

    /**
     * Find latest comments for a review
     */
    List<ReviewComments> findTop5ByReviewReviewIdOrderByCreatedAtDesc(Long reviewId);
}