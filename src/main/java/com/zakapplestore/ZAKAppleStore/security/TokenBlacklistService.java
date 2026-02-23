package com.zakapplestore.ZAKAppleStore.security;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expiration) {
        if (token == null || token.isBlank() || expiration == null) {
            return;
        }
        blacklistedTokens.put(token, expiration);
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Date expiration = blacklistedTokens.get(token);
        if (expiration == null) {
            return false;
        }
        if (expiration.before(new Date())) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }
}
