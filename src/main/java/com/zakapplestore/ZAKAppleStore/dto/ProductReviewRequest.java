package com.zakapplestore.ZAKAppleStore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductReviewRequest {

    @NotBlank(message = "Review title is required")
    @Size(max = 180, message = "Review title must be 180 characters or less")
    private String title;

    @NotBlank(message = "Review comment is required")
    @Size(max = 2000, message = "Review comment must be 2000 characters or less")
    private String comment;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int rating;
}
