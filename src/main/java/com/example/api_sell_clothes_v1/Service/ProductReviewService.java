package com.example.api_sell_clothes_v1.Service;

import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewSummaryDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentUpdateDTO;
import com.example.api_sell_clothes_v1.Entity.ProductReviews;
import com.example.api_sell_clothes_v1.Entity.Products;
import com.example.api_sell_clothes_v1.Entity.ReviewComments;
import com.example.api_sell_clothes_v1.Entity.Users;
import com.example.api_sell_clothes_v1.Exceptions.ResourceNotFoundException;
import com.example.api_sell_clothes_v1.Mapper.ProductReviewMapper;
import com.example.api_sell_clothes_v1.Mapper.ReviewCommentMapper;
import com.example.api_sell_clothes_v1.Repository.ProductRepository;
import com.example.api_sell_clothes_v1.Repository.ProductReviewRepository;
import com.example.api_sell_clothes_v1.Repository.ReviewCommentRepository;
import com.example.api_sell_clothes_v1.Repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductReviewMapper productReviewMapper;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewCommentMapper reviewCommentMapper;

    /**
     * Get product review by ID
     */
    @Transactional(readOnly = true)
    public ProductReviewResponseDTO getReviewById(Long reviewId) {
        ProductReviews review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        return productReviewMapper.toDto(review);
    }

    /**
     * Get all reviews for a product with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductReviewResponseDTO> getReviewsByProductId(Long productId, Pageable pageable) {
        // Check if product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Page<ProductReviews> reviewsPage = productReviewRepository.findByProductProductId(productId, pageable);
        return reviewsPage.map(productReviewMapper::toDto);
    }

    /**
     * Get all reviews by a user with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductReviewResponseDTO> getReviewsByUserId(Long userId, Pageable pageable) {
        // Check if user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<ProductReviews> reviewsPage = productReviewRepository.findByUserUserId(userId, pageable);
        return reviewsPage.map(productReviewMapper::toDto);
    }

    /**
     * Create a new product review
     * Now allowing multiple reviews per user for the same product
     */
    @Transactional
    public ProductReviewResponseDTO createReview(ProductReviewCreateDTO createDTO) {
        // Validate rating
        validateRating(createDTO.getRating());

        // Check if product exists
        Products product = productRepository.findById(createDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if user exists
        Users user = userRepository.findById(createDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create and save the review
        ProductReviews review = productReviewMapper.createDtoToEntity(createDTO, product, user);
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        ProductReviews savedReview = productReviewRepository.save(review);
        log.info("Created new review for product ID: {} by user ID: {}",
                savedReview.getProduct().getProductId(), savedReview.getUser().getUserId());

        return productReviewMapper.toDto(savedReview);
    }

    /**
     * Update an existing product review
     */
    @Transactional
    public ProductReviewResponseDTO updateReview(Long reviewId, Long userId, ProductReviewUpdateDTO updateDTO) {
        // Validate rating if provided
        if (updateDTO.getRating() != null) {
            validateRating(updateDTO.getRating());
        }

        // Find the review
        ProductReviews existingReview = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if the review belongs to the user
        if (!Objects.equals(existingReview.getUser().getUserId(), userId)) {
            throw new IllegalArgumentException("User is not authorized to update this review");
        }

        // Update the review
        productReviewMapper.updateEntityFromDto(updateDTO, existingReview);
        existingReview.setUpdatedAt(LocalDateTime.now());

        ProductReviews updatedReview = productReviewRepository.save(existingReview);
        log.info("Updated review ID: {} for product ID: {} by user ID: {}",
                updatedReview.getReviewId(), updatedReview.getProduct().getProductId(), userId);

        return productReviewMapper.toDto(updatedReview);
    }

    /**
     * Delete a product review
     */
    @Transactional
    public ApiResponse deleteReview(Long reviewId, Long userId) {
        // Find the review
        ProductReviews review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if the review belongs to the user or if user is an admin
        // For admin check, you would typically add role-based checks here
        if (!Objects.equals(review.getUser().getUserId(), userId)) {
            throw new IllegalArgumentException("User is not authorized to delete this review");
        }

        // First delete all comments associated with this review
        reviewCommentRepository.deleteAllByReviewReviewId(reviewId);

        // Then delete the review
        productReviewRepository.delete(review);
        log.info("Deleted review ID: {} for product ID: {} by user ID: {}",
                reviewId, review.getProduct().getProductId(), userId);

        return new ApiResponse(true, "Review successfully deleted");
    }

    /**
     * Get review summary for a product
     */
    @Transactional(readOnly = true)
    public ProductReviewSummaryDTO getProductReviewSummary(Long productId) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Double averageRating = productReviewRepository.calculateAverageRatingByProductId(productId);
        averageRating = averageRating != null ? averageRating : 0.0;

        Integer totalReviews = productReviewRepository.countByProductId(productId);
        Integer fiveStarCount = productReviewRepository.countByProductIdAndRating(productId, 5);
        Integer fourStarCount = productReviewRepository.countByProductIdAndRating(productId, 4);
        Integer threeStarCount = productReviewRepository.countByProductIdAndRating(productId, 3);
        Integer twoStarCount = productReviewRepository.countByProductIdAndRating(productId, 2);
        Integer oneStarCount = productReviewRepository.countByProductIdAndRating(productId, 1);

        List<ProductReviews> latestReviews = productReviewRepository
                .findTop5ByProductProductIdOrderByCreatedAtDesc(productId);

        return ProductReviewSummaryDTO.builder()
                .productId(productId)
                .productName(product.getName())
                .averageRating(Math.round(averageRating * 10.0) / 10.0) // Round to 1 decimal place
                .totalReviews(totalReviews)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .latestReviews(productReviewMapper.toDto(latestReviews))
                .build();
    }

    /**
     * Get user's reviews for a product
     */
    @Transactional(readOnly = true)
    public List<ProductReviewResponseDTO> getUserReviewsForProduct(Long productId, Long userId) {
        List<ProductReviews> reviews = productReviewRepository
                .findAllByProductProductIdAndUserUserId(productId, userId);
        return productReviewMapper.toDto(reviews);
    }

    /**
     * Check if a user has reviewed a product
     */
    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(Long productId, Long userId) {
        return productReviewRepository
                .existsByProductProductIdAndUserUserId(productId, userId);
    }

    /**
     * Get a review by product and user IDs (returns the most recent review)
     */
    @Transactional(readOnly = true)
    public ProductReviewResponseDTO getReviewByProductAndUser(Long productId, Long userId) {
        Optional<ProductReviews> review = productReviewRepository
                .findTopByProductProductIdAndUserUserIdOrderByCreatedAtDesc(productId, userId);

        if (review.isEmpty()) {
            throw new EntityNotFoundException("Review not found for this product and user");
        }

        return productReviewMapper.toDto(review.get());
    }

    // REVIEW COMMENTS METHODS

    /**
     * Add a comment to a review
     */
    @Transactional
    public ReviewCommentResponseDTO addCommentToReview(ReviewCommentCreateDTO createDTO) {
        // Check if review exists
        ProductReviews review = productReviewRepository.findById(createDTO.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check if user exists
        Users user = userRepository.findById(createDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create and save the comment
        ReviewComments comment = reviewCommentMapper.createDtoToEntity(createDTO, review, user);
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        ReviewComments savedComment = reviewCommentRepository.save(comment);
        log.info("Created new comment for review ID: {} by user ID: {}",
                savedComment.getReview().getReviewId(), savedComment.getUser().getUserId());

        return reviewCommentMapper.toDto(savedComment);
    }

    /**
     * Update a review comment
     */
    @Transactional
    public ReviewCommentResponseDTO updateComment(Long commentId, Long userId, ReviewCommentUpdateDTO updateDTO) {
        // Find the comment
        ReviewComments existingComment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Check if the comment belongs to the user
        if (!Objects.equals(existingComment.getUser().getUserId(), userId)) {
            throw new IllegalArgumentException("User is not authorized to update this comment");
        }

        // Update the comment
        reviewCommentMapper.updateEntityFromDto(updateDTO, existingComment);
        existingComment.setUpdatedAt(LocalDateTime.now());

        ReviewComments updatedComment = reviewCommentRepository.save(existingComment);
        log.info("Updated comment ID: {} for review ID: {} by user ID: {}",
                updatedComment.getCommentId(), updatedComment.getReview().getReviewId(), userId);

        return reviewCommentMapper.toDto(updatedComment);
    }

    /**
     * Delete a review comment
     */
    @Transactional
    public ApiResponse deleteComment(Long commentId, Long userId) {
        // Find the comment
        ReviewComments comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        // Check if the comment belongs to the user
        if (!Objects.equals(comment.getUser().getUserId(), userId)) {
            throw new IllegalArgumentException("User is not authorized to delete this comment");
        }

        reviewCommentRepository.delete(comment);
        log.info("Deleted comment ID: {} for review ID: {} by user ID: {}",
                commentId, comment.getReview().getReviewId(), userId);

        return new ApiResponse(true, "Comment successfully deleted");
    }

    /**
     * Get all comments for a review with pagination
     */
    @Transactional(readOnly = true)
    public Page<ReviewCommentResponseDTO> getCommentsByReviewId(Long reviewId, Pageable pageable) {
        // Check if review exists
        productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        Page<ReviewComments> commentsPage = reviewCommentRepository.findByReviewReviewId(reviewId, pageable);
        return commentsPage.map(reviewCommentMapper::toDto);
    }

    // Helper methods
    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
    }
}