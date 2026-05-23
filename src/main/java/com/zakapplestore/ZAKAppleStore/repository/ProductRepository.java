package com.zakapplestore.ZAKAppleStore.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakapplestore.ZAKAppleStore.dto.ProductRequest;
import com.zakapplestore.ZAKAppleStore.dto.ProductResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductSearchResponse;
import com.zakapplestore.ZAKAppleStore.dto.ProductSearchSuggestionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<ProductResponse> findAllAdmin() {
        String sql = baseSelectWithoutGallery() + """
                ORDER BY p.created_at DESC
                """;
        return jdbcTemplate.query(sql, productRowMapper(false));
    }

    public List<ProductResponse> findPublicProducts(String categoryName) {
        String sql = baseSelectWithoutGallery() + """
                WHERE p.status = 'Active'
                  AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName))
                ORDER BY p.created_at DESC
                """;
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("categoryName", isBlank(categoryName) ? null : categoryName.trim()),
                productRowMapper(false)
        );
    }

    public List<ProductResponse> findNewArrivals(int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", Math.max(1, limit));

        String sql = baseSelectWithoutGallery() + """
                WHERE p.status = 'Active'
                ORDER BY p.created_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                params,
                productRowMapper(false)
        );
    }

    public Optional<ProductResponse> findById(UUID id) {
        String sql = baseSelectWithGallery() + """
                WHERE p.id = :id
                """;
        List<ProductResponse> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("id", id),
                productRowMapper(true)
        );
        return rows.stream().findFirst();
    }

    public List<ProductResponse> findBestSellers(int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", Math.max(1, limit));

        String sql = baseSelectWithoutGallery() + """
                WHERE p.status = 'Active'
                  AND p.best_seller = TRUE
                ORDER BY p.created_at DESC
                LIMIT :limit
                """;

        return jdbcTemplate.query(sql, params, productRowMapper(false));
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        String sql = """
                UPDATE products
                SET category_id = :categoryId,
                    product_name = :productName,
                    product_description = :productDescription,
                    mrp = :mrp,
                    price = :price,
                    main_photo = :mainPhoto,
                    photo_gallery_json = :photoGalleryJson,
                    best_seller = :bestSeller,
                    storage = :storage,
                    color = :color,
                    stock_quantity = :stockQuantity,
                    updated_at = :updatedAt
                WHERE id = :id
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("categoryId", request.getCategoryId())
                        .addValue("productName", request.getProductName())
                        .addValue("productDescription", request.getProductDescription())
                        .addValue("mrp", safeAmount(request.getMrp()))
                        .addValue("price", safeAmount(request.getPrice()))
                        .addValue("mainPhoto", request.getMainPhoto())
                        .addValue("photoGalleryJson", writePhotoGallery(request.getPhotoGallery()))
                        .addValue("bestSeller", request.getBestSeller())
                        .addValue("storage", request.getStorage())
                        .addValue("color", request.getColor())
                        .addValue("stockQuantity", request.getStockQuantity())
                        .addValue("updatedAt", LocalDateTime.now())
        );

        return findById(id).orElseThrow();
    }

    public Optional<ProductResponse> findByIdWithPessimisticLock(UUID id) {
        String sql = baseSelectWithGallery() + """
                WHERE p.id = :id FOR UPDATE
                """;
        List<ProductResponse> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("id", id),
                productRowMapper(true)
        );
        return rows.stream().findFirst();
    }

    public void updateStock(UUID id, int newStock) {
        String sql = """
                UPDATE products
                SET stock_quantity = :stock,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """;
        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("stock", newStock)
        );
    }

    public boolean existsByCategoryId(UUID categoryId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM products
                    WHERE category_id = :categoryId
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("categoryId", categoryId),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    public ProductResponse save(ProductRequest request) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String productCode = generateNextProductCode();

        String sql = """
                INSERT INTO products (
                    id,
                    product_code,
                    category_id,
                    product_name,
                    product_description,
                    mrp,
                    price,
                    main_photo,
                    photo_gallery_json,
                    best_seller,
                    storage,
                    color,
                    stock_quantity,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    :id,
                    :productCode,
                    :categoryId,
                    :productName,
                    :productDescription,
                    :mrp,
                    :price,
                    :mainPhoto,
                    :photoGalleryJson,
                    :bestSeller,
                    :storage,
                    :color,
                    :stockQuantity,
                    :status,
                    :createdAt,
                    :updatedAt
                )
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("productCode", productCode)
                        .addValue("categoryId", request.getCategoryId())
                        .addValue("productName", request.getProductName())
                        .addValue("productDescription", request.getProductDescription())
                        .addValue("mrp", safeAmount(request.getMrp()))
                        .addValue("price", safeAmount(request.getPrice()))
                        .addValue("mainPhoto", request.getMainPhoto())
                        .addValue("photoGalleryJson", writePhotoGallery(request.getPhotoGallery()))
                        .addValue("bestSeller", request.getBestSeller())
                        .addValue("storage", request.getStorage())
                        .addValue("color", request.getColor())
                        .addValue("stockQuantity", request.getStockQuantity())
                        .addValue("status", "Active")
                        .addValue("createdAt", now)
                        .addValue("updatedAt", now)
        );

        return findById(id).orElseThrow();
    }

    public ProductResponse updateStatus(UUID id, String status) {
        String sql = """
                UPDATE products
                SET status = :status,
                    updated_at = :updatedAt
                WHERE id = :id
                """;
        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("status", status)
                        .addValue("updatedAt", LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }

    public ProductSearchResponse searchPublicProducts(String query, String sort, int page, int size) {
        String normalizedQuery = normalizeSearchQuery(query);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 24);
        int offset = safePage * safeSize;
        String[] tokens = normalizedQuery.toLowerCase().split("\\s+");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", normalizedQuery)
                .addValue("size", safeSize)
                .addValue("offset", offset);

        StringBuilder tokenClause = new StringBuilder();
        for (int index = 0; index < tokens.length; index++) {
            String tokenKey = "token" + index;
            params.addValue(tokenKey, "%" + tokens[index] + "%");
            tokenClause.append("""
                    AND (
                        LOWER(p.product_name) LIKE LOWER(:%s)
                        OR LOWER(p.product_description) LIKE LOWER(:%s)
                        OR LOWER(c.name) LIKE LOWER(:%s)
                    )
                    """.formatted(tokenKey, tokenKey, tokenKey));
        }

        String baseWhere = """
                WHERE p.status = 'Active'
                """ + tokenClause;

        String sql = """
                SELECT
                    p.id,
                    p.product_code,
                    p.category_id,
                    c.name AS category_name,
                    p.product_name,
                    p.product_description,
                    p.mrp,
                    p.price,
                    p.main_photo,
                    '[]' AS photo_gallery_json,
                    p.stock_quantity,
                    p.status,
                    p.created_at,
                    p.updated_at
                FROM products p
                JOIN categories c ON c.id = p.category_id
                """ + baseWhere + """
                ORDER BY
                """ + buildSearchSortSql(sort, normalizedQuery) + """
                LIMIT :size OFFSET :offset
                """;

        List<ProductResponse> products = jdbcTemplate.query(sql, params, productRowMapper(false));

        Long totalElements = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM products p
                        JOIN categories c ON c.id = p.category_id
                        """ + baseWhere,
                params,
                Long.class
        );

        List<String> categories = jdbcTemplate.query(
                """
                        SELECT DISTINCT c.name
                        FROM products p
                        JOIN categories c ON c.id = p.category_id
                        """ + baseWhere + """
                        ORDER BY c.name ASC
                        LIMIT 10
                        """,
                params,
                (rs, rowNum) -> rs.getString("name")
        );

        List<String> suggestions = findSuggestions(normalizedQuery).getProducts();
        long total = totalElements == null ? 0L : totalElements;

        return ProductSearchResponse.builder()
                .products(products)
                .categories(categories)
                .suggestions(suggestions)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages((int) Math.ceil(total / (double) safeSize))
                .sort(normalizeSort(sort))
                .query(normalizedQuery)
                .build();
    }

    public ProductSearchSuggestionsResponse findSuggestions(String query) {
        String normalizedQuery = normalizeSearchQuery(query);
        String pattern = normalizedQuery.isEmpty() ? "%" : normalizedQuery + "%";

        List<String> productSuggestions = jdbcTemplate.query(
                """
                        SELECT product_name
                        FROM products
                        WHERE status = 'Active'
                          AND LOWER(product_name) LIKE LOWER(:pattern)
                        ORDER BY
                          CASE WHEN LOWER(product_name) = LOWER(:query) THEN 0 ELSE 1 END,
                          product_name ASC
                        LIMIT 6
                        """,
                new MapSqlParameterSource()
                        .addValue("query", normalizedQuery)
                        .addValue("pattern", pattern),
                (rs, rowNum) -> rs.getString("product_name")
        );

        List<String> categorySuggestions = jdbcTemplate.query(
                """
                        SELECT DISTINCT name
                        FROM categories
                        WHERE LOWER(name) LIKE LOWER(:pattern)
                        ORDER BY
                          CASE WHEN LOWER(name) = LOWER(:query) THEN 0 ELSE 1 END,
                          name ASC
                        LIMIT 5
                        """,
                new MapSqlParameterSource()
                        .addValue("query", normalizedQuery)
                        .addValue("pattern", pattern),
                (rs, rowNum) -> rs.getString("name")
        );

        List<String> trending = jdbcTemplate.query(
                """
                        SELECT product_name
                        FROM products
                        WHERE status = 'Active'
                        ORDER BY created_at DESC
                        LIMIT 5
                        """,
                (rs, rowNum) -> rs.getString("product_name")
        );

        if (productSuggestions.isEmpty() && !normalizedQuery.isEmpty()) {
            productSuggestions = buildFuzzySuggestions(
                    normalizedQuery,
                    jdbcTemplate.query(
                            """
                                    SELECT DISTINCT product_name
                                    FROM products
                                    WHERE status = 'Active'
                                    ORDER BY created_at DESC
                                    LIMIT 60
                                    """,
                            (rs, rowNum) -> rs.getString("product_name")
                    )
            );
        }

        if (categorySuggestions.isEmpty() && !normalizedQuery.isEmpty()) {
            categorySuggestions = buildFuzzySuggestions(
                    normalizedQuery,
                    jdbcTemplate.query(
                            """
                                    SELECT DISTINCT name
                                    FROM categories
                                    ORDER BY name ASC
                                    LIMIT 30
                                    """,
                            (rs, rowNum) -> rs.getString("name")
                    )
            );
        }

        return ProductSearchSuggestionsResponse.builder()
                .products(productSuggestions)
                .categories(categorySuggestions)
                .trending(trending)
                .recent(Collections.emptyList())
                .query(normalizedQuery)
                .build();
    }

    private String baseSelectWithoutGallery() {
        return """
                SELECT
                    p.id,
                    p.product_code,
                    p.category_id,
                    c.name AS category_name,
                    p.product_name,
                    p.product_description,
                    p.mrp,
                    p.price,
                    p.main_photo,
                    p.best_seller,
                    p.storage,
                    p.color,
                    '[]' AS photo_gallery_json,
                    p.stock_quantity,
                    p.status,
                    p.created_at,
                    p.updated_at
                FROM products p
                JOIN categories c ON c.id = p.category_id
                """;
    }

    private String buildSearchSortSql(String sort, String normalizedQuery) {
        String normalizedSort = normalizeSort(sort);
        if ("price_asc".equals(normalizedSort)) {
            return "p.price ASC, p.created_at DESC";
        }

        if ("price_desc".equals(normalizedSort)) {
            return "p.price DESC, p.created_at DESC";
        }

        if ("newest".equals(normalizedSort)) {
            return "p.created_at DESC";
        }

        return """
                CASE
                    WHEN LOWER(p.product_name) = LOWER('%s') THEN 1000
                    WHEN LOWER(p.product_name) LIKE LOWER('%s' || '%%') THEN 700
                    WHEN LOWER(c.name) = LOWER('%s') THEN 500
                    WHEN LOWER(p.product_name) LIKE LOWER('%%' || '%s' || '%%') THEN 350
                    WHEN LOWER(c.name) LIKE LOWER('%%' || '%s' || '%%') THEN 220
                    WHEN LOWER(p.product_description) LIKE LOWER('%%' || '%s' || '%%') THEN 120
                    ELSE 0
                END DESC,
                p.created_at DESC
                """.formatted(
                escapeSqlLiteral(normalizedQuery),
                escapeSqlLiteral(normalizedQuery),
                escapeSqlLiteral(normalizedQuery),
                escapeSqlLiteral(normalizedQuery),
                escapeSqlLiteral(normalizedQuery),
                escapeSqlLiteral(normalizedQuery)
        );
    }

    private String baseSelectWithGallery() {
        return """
                SELECT
                    p.id,
                    p.product_code,
                    p.category_id,
                    c.name AS category_name,
                    p.product_name,
                    p.product_description,
                    p.mrp,
                    p.price,
                    p.main_photo,
                    p.best_seller,
                    p.storage,
                    p.color,
                    p.photo_gallery_json,
                    p.stock_quantity,
                    p.status,
                    p.created_at,
                    p.updated_at
                FROM products p
                JOIN categories c ON c.id = p.category_id
                """;
    }

    private RowMapper<ProductResponse> productRowMapper(boolean includeGallery) {
        return new RowMapper<>() {
            @Override
            public ProductResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");

                return ProductResponse.builder()
                        .id(rs.getObject("id", UUID.class))
                        .productId(rs.getString("product_code"))
                        .categoryId(rs.getObject("category_id", UUID.class))
                        .category(rs.getString("category_name"))
                        .productName(rs.getString("product_name"))
                        .productDescription(rs.getString("product_description"))
                        .mrp(rs.getBigDecimal("mrp"))
                        .price(rs.getBigDecimal("price"))
                        .mainPhoto(rs.getString("main_photo"))
                        .bestSeller(rs.getBoolean("best_seller"))
                        .storage(rs.getString("storage"))
                        .color(rs.getString("color"))
                        .photoGallery(includeGallery ? readPhotoGallery(rs.getString("photo_gallery_json")) : Collections.emptyList())
                        .stockQuantity(rs.getInt("stock_quantity"))
                        .status(rs.getString("status"))
                        .createdAt(createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : LocalDateTime.now())
                        .updatedAt(updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : LocalDateTime.now())
                        .build();
            }
        };
    }

    private String generateNextProductCode() {
        Long nextValue = jdbcTemplate.getJdbcTemplate().queryForObject("SELECT nextval('products_code_seq')", Long.class);
        long value = nextValue == null ? 1L : nextValue;
        return "PRD-" + String.format("%04d", value);
    }

    private String writePhotoGallery(List<String> photoGallery) {
        try {
            return objectMapper.writeValueAsString(photoGallery == null ? Collections.emptyList() : photoGallery);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize product gallery", exception);
        }
    }

    private List<String> readPhotoGallery(String rawJson) {
        if (isBlank(rawJson)) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(rawJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeSearchQuery(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSort(String value) {
        if (isBlank(value)) {
            return "relevance";
        }

        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "price_asc", "price_desc", "newest", "relevance" -> normalized;
            default -> "relevance";
        };
    }

    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private List<String> buildFuzzySuggestions(String query, List<String> candidates) {
        return candidates.stream()
                .filter(candidate -> !isBlank(candidate))
                .sorted((left, right) -> Integer.compare(levenshtein(query.toLowerCase(), left.toLowerCase()), levenshtein(query.toLowerCase(), right.toLowerCase())))
                .limit(5)
                .toList();
    }

    private int levenshtein(String left, String right) {
        int[][] distance = new int[left.length() + 1][right.length() + 1];

        for (int i = 0; i <= left.length(); i++) {
            distance[i][0] = i;
        }

        for (int j = 0; j <= right.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                distance[i][j] = Math.min(
                        Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                        distance[i - 1][j - 1] + cost
                );
            }
        }

        return distance[left.length()][right.length()];
    }
}
