package com.zakapplestore.ZAKAppleStore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {
    private UUID productId;
    private Integer quantity;
    // We only use this for frontend caching or passing around. 
    // Backend logic MUST always recalculate price from the DB.
    private BigDecimal priceSnapshot; 
}
