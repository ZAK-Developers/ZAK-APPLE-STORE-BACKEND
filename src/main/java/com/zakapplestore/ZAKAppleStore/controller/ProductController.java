package com.zakapplestore.ZAKAppleStore.controller;

import com.zakapplestore.ZAKAppleStore.dto.ProductRequest;
import com.zakapplestore.ZAKAppleStore.dto.ProductResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductSearchResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductSearchSuggestionsResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductStatusRequest;
import com.zakapplestore.ZAKAppleStore.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getPublicProducts(
            @RequestParam(value = "category", required = false) String category
    ) {
        return ResponseEntity.ok(productService.getPublicProducts(category));
    }

    @GetMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestParam("q") String query,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(productService.searchPublicProducts(query, sort, page, size));
    }

    @GetMapping("/search/suggestions")
    public ResponseEntity<ProductSearchSuggestionsResponse> getSearchSuggestions(
            @RequestParam(value = "q", required = false) String query
    ) {
        return ResponseEntity.ok(productService.getSearchSuggestions(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductResponse>> getAdminProducts() {
        return ResponseEntity.ok(productService.getAdminProducts());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        return ResponseEntity.ok(productService.updateProductStatus(id, request.getStatus()));
    }
}
