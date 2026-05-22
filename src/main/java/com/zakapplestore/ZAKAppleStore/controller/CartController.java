package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.CartAddRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartItemQuantityRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartMergeRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartResponse;
import com.zakapplestore.ZAKAppleStore.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(authentication.getName()));
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            Authentication authentication,
            @Valid @RequestBody CartAddRequest request
    ) {
        return ResponseEntity.ok(cartService.addToCart(authentication.getName(), request));
    }

    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeCart(
            Authentication authentication,
            @Valid @RequestBody CartMergeRequest request
    ) {
        return ResponseEntity.ok(cartService.mergeCart(authentication.getName(), request));
    }

    @PutMapping("/item/{id}")
    public ResponseEntity<CartResponse> updateCartItem(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CartItemQuantityRequest request
    ) {
        return ResponseEntity.ok(cartService.updateQuantity(authentication.getName(), id, request));
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<CartResponse> removeCartItem(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(cartService.removeItem(authentication.getName(), id));
    }
}
