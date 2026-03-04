package com.zakapplestore.ZAKAppleStore.dto;

import com.zakapplestore.ZAKAppleStore.entity.LoginStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistoryResponse {

    private UUID id;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String device;
    private LoginStatus status;
}
