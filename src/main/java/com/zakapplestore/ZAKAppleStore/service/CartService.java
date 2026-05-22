package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.CartAddRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartItemQuantityRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartMergeRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartResponse;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.CartRepository;
import com.zakapplestore.ZAKAppleStore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CART_CACHE_PREFIX = "cart_cache:";
    private static final long CACHE_EXPIRY_HOURS = 24;

    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        User user = getUser(email);
        String cacheKey = CART_CACHE_PREFIX + user.getId();
        
        try {
            CartResponse cached = (CartResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            // Log warning in real app; ignore cache failure
        }

        CartResponse response = cartRepository.getCart(user.getId());
        
        try {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_EXPIRY_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // Ignore cache failure
        }
        
        return response;
    }

    @Transactional
    public CartResponse addToCart(String email, CartAddRequest request) {
        User user = getUser(email);
        CartResponse response = cartRepository.addItem(
                user.getId(),
                request.getProductId(),
                request.getQuantity(),
                normalize(request.getColor()),
                normalize(request.getStorage())
        );
        invalidateCache(user.getId());
        return response;
    }

    @Transactional
    public CartResponse updateQuantity(String email, UUID cartItemId, CartItemQuantityRequest request) {
        User user = getUser(email);
        CartResponse response = cartRepository.updateQuantity(user.getId(), cartItemId, request.getQuantity());
        invalidateCache(user.getId());
        return response;
    }

    @Transactional
    public CartResponse removeItem(String email, UUID cartItemId) {
        User user = getUser(email);
        CartResponse response = cartRepository.removeItem(user.getId(), cartItemId);
        invalidateCache(user.getId());
        return response;
    }

    @Transactional
    public CartResponse mergeCart(String email, CartMergeRequest request) {
        User user = getUser(email);
        CartResponse response = cartRepository.mergeItems(user.getId(), request.getItems());
        invalidateCache(user.getId());
        return response;
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
    
    private void invalidateCache(UUID userId) {
        try {
            redisTemplate.delete(CART_CACHE_PREFIX + userId);
        } catch (Exception e) {
            // Ignore cache failure
        }
    }
}
