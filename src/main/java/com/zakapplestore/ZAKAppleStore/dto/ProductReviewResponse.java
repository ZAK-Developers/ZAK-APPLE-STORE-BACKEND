package com.zakapplestore.ZAKAppleStore.dto;

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
public class ProductReviewResponse {

    private UUID id;
    private UUID productId;
    private UUID userId;
    private String author;
    private String title;
    private String comment;
    private int rating;
    private LocalDateTime createdAt;
}
