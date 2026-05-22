package com.zakapplestore.ZAKAppleStore.dto;

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
public class ProductSearchResponse {

    private List<ProductResponse> products;
    private List<String> categories;
    private List<String> suggestions;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String sort;
    private String query;
}
