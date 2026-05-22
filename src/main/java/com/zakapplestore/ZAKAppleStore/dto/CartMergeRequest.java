package com.zakapplestore.ZAKAppleStore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartMergeRequest {

    @Valid
    @NotNull(message = "Items are required")
    private List<CartMergeItemRequest> items;
}
