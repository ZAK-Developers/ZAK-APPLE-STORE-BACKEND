package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.LoginHistoryResponse;
import com.zakapplestore.ZAKAppleStore.entity.LoginHistory;
import com.zakapplestore.ZAKAppleStore.entity.LoginStatus;
import com.zakapplestore.ZAKAppleStore.entity.User;
import com.zakapplestore.ZAKAppleStore.repository.LoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    public void recordLogin(User user, HttpServletRequest request, LoginStatus status) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .ipAddress(resolveIpAddress(request))
                .device(resolveDevice(request))
                .status(status)
                .build();
        loginHistoryRepository.save(history);
    }

    public List<LoginHistoryResponse> getRecentLogins(User user) {
        return loginHistoryRepository.findTop20ByUserIdOrderByLoginTimeDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private LoginHistoryResponse mapToResponse(LoginHistory entity) {
        return LoginHistoryResponse.builder()
                .id(entity.getId())
                .loginTime(entity.getLoginTime())
                .ipAddress(entity.getIpAddress())
                .device(entity.getDevice())
                .status(entity.getStatus())
                .build();
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveDevice(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? "UNKNOWN" : userAgent;
    }
}
