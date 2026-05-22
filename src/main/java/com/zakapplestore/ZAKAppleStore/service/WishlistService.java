package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.WishlistAddRequest;
import com.zakapplestore.ZAKAppleStore.dto.WishlistItemResponse;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import com.zakapplestore.ZAKAppleStore.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String email) {
        return wishlistRepository.getWishlist(getUser(email).getId());
    }

    @Transactional
    public List<WishlistItemResponse> addToWishlist(String email, WishlistAddRequest request) {
        return wishlistRepository.addItem(getUser(email).getId(), request.getProductId());
    }

    @Transactional
    public List<WishlistItemResponse> removeFromWishlist(String email, UUID productId) {
        return wishlistRepository.removeItem(getUser(email).getId(), productId);
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
