package com.zakapplestore.ZAKAppleStore.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 80, message = "Product name must be between 2 and 80 characters")
    private String productName;

    @NotBlank(message = "Product description is required")
    @Size(min = 8, max = 500, message = "Product description must be between 8 and 500 characters")
    private String productDescription;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "MRP must be 0 or greater")
    private BigDecimal mrp;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be 0 or greater")
    private BigDecimal price;

    @NotBlank(message = "Main product photo is required")
    private String mainPhoto;

    private List<String> photoGallery;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be 0 or greater")
    private Integer stockQuantity;
}
