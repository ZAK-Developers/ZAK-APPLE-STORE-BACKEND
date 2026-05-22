package com.zakapplestore.ZAKAppleStore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse implements Serializable {

    private UUID id;
    private UUID productId;
    private String productCode;
    private String productName;
    private String category;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal mrp;
    private String image;
    private String color;
    private String storage;
}
