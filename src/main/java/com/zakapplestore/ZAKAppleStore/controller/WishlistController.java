package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.WishlistAddRequest;
import com.zakapplestore.ZAKAppleStore.dto.WishlistItemResponse;
import com.zakapplestore.ZAKAppleStore.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlist(authentication.getName()));
    }

    @PostMapping("/add")
    public ResponseEntity<List<WishlistItemResponse>> addToWishlist(
            Authentication authentication,
            @Valid @RequestBody WishlistAddRequest request
    ) {
        return ResponseEntity.ok(wishlistService.addToWishlist(authentication.getName(), request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<List<WishlistItemResponse>> removeFromWishlist(
            Authentication authentication,
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(wishlistService.removeFromWishlist(authentication.getName(), productId));
    }
}
