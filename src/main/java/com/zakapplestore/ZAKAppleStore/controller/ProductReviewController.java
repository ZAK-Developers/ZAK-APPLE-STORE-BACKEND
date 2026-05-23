package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.MessageResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductReviewRequest;
import com.zakapplestore.ZAKAppleStore.dto.ProductReviewResponse;
import com.zakapplestore.ZAKAppleStore.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<List<ProductReviewResponse>> getProductReviews(@PathVariable UUID productId) {
        return ResponseEntity.ok(productReviewService.getProductReviews(productId));
    }

    @PostMapping("/{productId}/reviews")
    public ResponseEntity<ProductReviewResponse> addReview(
            @PathVariable UUID productId,
            Authentication authentication,
            @Valid @RequestBody ProductReviewRequest request
    ) {
        return ResponseEntity.ok(productReviewService.addReview(productId, authentication.getName(), request));
    }

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductReviewResponse>> getAllReviewsForAdmin() {
        return ResponseEntity.ok(productReviewService.getAllReviewsForAdmin());
    }

    @DeleteMapping("/admin/reviews/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteReview(@PathVariable UUID reviewId) {
        productReviewService.deleteReview(reviewId);
        return ResponseEntity.ok(MessageResponse.builder().message("Review deleted successfully").build());
    }
}
