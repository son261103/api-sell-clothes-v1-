package com.example.api_sell_clothes_v1.Controller.User;

import com.example.api_sell_clothes_v1.Constants.ApiPatternConstants;
import com.example.api_sell_clothes_v1.DTO.ApiResponse;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewSummaryDTO;
import com.example.api_sell_clothes_v1.DTO.ProductReviews.ProductReviewUpdateDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentCreateDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentResponseDTO;
import com.example.api_sell_clothes_v1.DTO.ReviewComments.ReviewCommentUpdateDTO;
import com.example.api_sell_clothes_v1.Security.CustomUserDetails;
import com.example.api_sell_clothes_v1.Service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPatternConstants.API_PUBLIC + "/reviews")
@RequiredArgsConstructor
@Slf4j
public class UserProductReviewController {

    private final ProductReviewService productReviewService;

    /**
     * Get reviews for a specific product with pagination
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ProductReviewResponseDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductReviewResponseDTO> reviews = productReviewService.getReviewsByProductId(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review summary for a product
     */
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ProductReviewSummaryDTO> getProductReviewSummary(
            @PathVariable Long productId) {
        ProductReviewSummaryDTO summary = productReviewService.getProductReviewSummary(productId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get a specific review by ID
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ProductReviewResponseDTO> getReviewById(
            @PathVariable Long reviewId) {
        ProductReviewResponseDTO review = productReviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Submit a new review for a product
     */
    @PostMapping
    public ResponseEntity<ProductReviewResponseDTO> createReview(
            @RequestBody ProductReviewCreateDTO createDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Extract the user ID from userDetails
        Long userId = extractUserId(userDetails);

        // Set the userId in the DTO
        createDTO.setUserId(userId);

        // Log the creation attempt
        log.info("Creating review for product {} by user {}", createDTO.getProductId(), userId);

        try {
            ProductReviewResponseDTO createdReview = productReviewService.createReview(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
        } catch (Exception e) {
            log.error("Error creating review: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update an existing review
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<ProductReviewResponseDTO> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ProductReviewUpdateDTO updateDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ProductReviewResponseDTO updatedReview = productReviewService.updateReview(reviewId, userId, updateDTO);
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Delete a review
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ApiResponse response = productReviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all reviews created by the current user
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<Page<ProductReviewResponseDTO>> getUserReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ProductReviewResponseDTO> reviews = productReviewService.getReviewsByUserId(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Check if the current user has already reviewed a product
     */
    @GetMapping("/product/{productId}/has-reviewed")
    public ResponseEntity<Map<String, Boolean>> hasUserReviewedProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        boolean hasReviewed = productReviewService.hasUserReviewedProduct(productId, userId);
        return ResponseEntity.ok(Map.of("hasReviewed", hasReviewed));
    }

    /**
     * Get all user's reviews for a specific product (NEW)
     */
    @GetMapping("/product/{productId}/my-reviews")
    public ResponseEntity<?> getUserReviewsForProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);

        try {
            List<ProductReviewResponseDTO> reviews = productReviewService.getUserReviewsForProduct(productId, userId);
            return ResponseEntity.ok(Map.of(
                    "hasReviews", !reviews.isEmpty(),
                    "reviews", reviews
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasReviews", false, "message", "No reviews found for this product"));
        }
    }

    /**
     * Get the current user's review for a specific product (OLD - for backwards compatibility)
     */
    @GetMapping("/product/{productId}/my-review")
    public ResponseEntity<?> getUserReviewForProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);

        try {
            ProductReviewResponseDTO review = productReviewService.getReviewByProductAndUser(productId, userId);
            return ResponseEntity.ok(Map.of(
                    "hasReview", true,
                    "review", review
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasReview", false, "message", "No review found for this product"));
        }
    }

    /**
     * Get the current user's most recent review for a specific product
     */
    @GetMapping("/product/{productId}/my-latest-review")
    public ResponseEntity<?> getUserLatestReviewForProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);

        try {
            ProductReviewResponseDTO review = productReviewService.getReviewByProductAndUser(productId, userId);
            return ResponseEntity.ok(Map.of(
                    "hasReview", true,
                    "review", review
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasReview", false, "message", "No review found for this product"));
        }
    }

    /**
     * Get comments for a specific review with pagination
     */
    @GetMapping("/{reviewId}/comments")
    public ResponseEntity<Page<ReviewCommentResponseDTO>> getReviewComments(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        // Create pageable with sorting
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReviewCommentResponseDTO> comments = productReviewService.getCommentsByReviewId(reviewId, pageable);
        return ResponseEntity.ok(comments);
    }

    /**
     * Add a comment to a review
     */
    @PostMapping("/{reviewId}/comments")
    public ResponseEntity<ReviewCommentResponseDTO> addComment(
            @PathVariable Long reviewId,
            @RequestBody ReviewCommentCreateDTO createDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);

        // Set the reviewId and userId in the DTO
        createDTO.setReviewId(reviewId);
        createDTO.setUserId(userId);

        ReviewCommentResponseDTO createdComment = productReviewService.addCommentToReview(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    /**
     * Update a review comment
     */
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ReviewCommentResponseDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody ReviewCommentUpdateDTO updateDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ReviewCommentResponseDTO updatedComment = productReviewService.updateComment(commentId, userId, updateDTO);
        return ResponseEntity.ok(updatedComment);
    }

    /**
     * Delete a review comment
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        ApiResponse response = productReviewService.deleteComment(commentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to extract user ID from CustomUserDetails
     */
    private Long extractUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("User is not authenticated. Using default user ID.");
            return 1L; // Fallback for unauthenticated requests
        }
        return userDetails.getUser().getUserId();
    }
}