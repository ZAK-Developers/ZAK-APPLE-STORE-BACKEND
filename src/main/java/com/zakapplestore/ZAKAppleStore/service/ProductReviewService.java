package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.ProductReviewRequest;
import com.zakapplestore.ZAKAppleStore.dto.ProductReviewResponse;
import com.zakapplestore.ZAKAppleStore.dto.UserProfileResponse;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductService productService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getProductReviews(UUID productId) {
        productService.getProductById(productId);
        return productReviewRepository.findByProductId(productId);
    }

    @Transactional
    public ProductReviewResponse addReview(UUID productId, String userEmail, ProductReviewRequest request) {
        productService.getProductById(productId);
        UserProfileResponse currentUser = userService.getProfile(userEmail);

        String title = normalize(request.getTitle());
        String comment = normalize(request.getComment());
        if (title.isEmpty()) {
            throw new BadRequestException("Review title is required");
        }
        if (comment.isEmpty()) {
            throw new BadRequestException("Review comment is required");
        }

        String author = normalize(currentUser.getUsername()).isEmpty() ? currentUser.getEmail() : currentUser.getUsername();
        return productReviewRepository.save(productId, currentUser.getId(), author, title, comment, request.getRating());
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getAllReviewsForAdmin() {
        return productReviewRepository.findAllAdmin();
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        productReviewRepository.deleteById(reviewId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
