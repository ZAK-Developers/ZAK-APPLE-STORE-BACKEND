package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.ProductRequest;
import com.zakapplestore.ZAKAppleStore.dto.ProductResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductSearchResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductSearchSuggestionsResponse;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.CategoryRepository;
import com.zakapplestore.ZAKAppleStore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String ACTIVE_STATUS = "Active";
    private static final String INACTIVE_STATUS = "Inactive";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAdminProducts() {
        return productRepository.findAllAdmin();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getPublicProducts(String categoryName) {
        return productRepository.findPublicProducts(categoryName);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    @Transactional(readOnly = true)
    public ProductSearchResponse searchPublicProducts(String query, String sort, int page, int size) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < 2) {
            throw new BadRequestException("Search query must be at least 2 characters.");
        }

        return productRepository.searchPublicProducts(normalizedQuery, sort, page, size);
    }

    @Transactional(readOnly = true)
    public ProductSearchSuggestionsResponse getSearchSuggestions(String query) {
        return productRepository.findSuggestions(normalize(query));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateCategoryExists(request.getCategoryId());
        validatePricing(request.getMrp(), request.getPrice());

        request.setProductName(normalize(request.getProductName()));
        request.setProductDescription(normalize(request.getProductDescription()));
        request.setMainPhoto(normalize(request.getMainPhoto()));

        return productRepository.save(request);
    }

    @Transactional
    public ProductResponse updateProductStatus(UUID id, String status) {
        getProductById(id);
        String normalizedStatus = normalizeStatus(status);
        return productRepository.updateStatus(id, normalizedStatus);
    }

    public boolean hasProductsForCategory(UUID categoryId) {
        return productRepository.existsByCategoryId(categoryId);
    }

    private void validateCategoryExists(UUID categoryId) {
        if (categoryId == null || categoryRepository.findById(categoryId).isEmpty()) {
            throw new BadRequestException("Selected category does not exist");
        }
    }

    private void validatePricing(BigDecimal mrp, BigDecimal price) {
        if (mrp == null || price == null) {
            throw new BadRequestException("MRP and price are required");
        }

        if (price.compareTo(mrp) > 0) {
            throw new BadRequestException("Price cannot be greater than MRP.");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (ACTIVE_STATUS.equalsIgnoreCase(normalized)) {
            return ACTIVE_STATUS;
        }

        if (INACTIVE_STATUS.equalsIgnoreCase(normalized)) {
            return INACTIVE_STATUS;
        }

        throw new BadRequestException("Invalid product status");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
