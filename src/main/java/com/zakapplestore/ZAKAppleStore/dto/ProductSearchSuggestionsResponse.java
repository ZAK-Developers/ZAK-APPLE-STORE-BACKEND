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
public class ProductSearchSuggestionsResponse {

    private List<String> products;
    private List<String> categories;
    private List<String> trending;
    private List<String> recent;
    private String query;
}
