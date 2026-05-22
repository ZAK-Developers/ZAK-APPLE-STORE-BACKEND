package com.zakapplestore.ZAKAppleStore.service;

import com.zakapplestore.ZAKAppleStore.dto.CategoryRequest;
import com.zakapplestore.ZAKAppleStore.dto.CategoryResponse;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import com.zakapplestore.ZAKAppleStore.repository.CategoryRepository;
import com.zakapplestore.ZAKAppleStore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int MAX_CATEGORY_LIMIT = 8;
    private static final String CATEGORY_LIMIT_MESSAGE = "Can't add categories more than 8.";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String categoryName = normalizeName(request.getName());

        if (categoryRepository.count() >= MAX_CATEGORY_LIMIT) {
            throw new BadRequestException(CATEGORY_LIMIT_MESSAGE);
        }

        if (categoryRepository.existsByNameIgnoreCase(categoryName)) {
            throw new BadRequestException("Category already exists");
        }

        return categoryRepository.save(categoryName);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        String categoryName = normalizeName(request.getName());
        getCategoryById(id);

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(categoryName, id)) {
            throw new BadRequestException("Category already exists");
        }

        return categoryRepository.update(id, categoryName);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        getCategoryById(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete category with existing products.");
        }
        categoryRepository.deleteById(id);
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
