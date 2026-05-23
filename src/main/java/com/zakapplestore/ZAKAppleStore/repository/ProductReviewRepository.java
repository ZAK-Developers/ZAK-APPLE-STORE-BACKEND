package com.zakapplestore.ZAKAppleStore.repository;

import com.zakapplestore.ZAKAppleStore.dto.ProductReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductReviewRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<ProductReviewResponse> findByProductId(UUID productId) {
        String sql = baseSelect() + """
                WHERE pr.product_id = :productId
                ORDER BY pr.created_at DESC, pr.id DESC
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("productId", productId),
                rowMapper()
        );
    }

    public List<ProductReviewResponse> findAllAdmin() {
        String sql = baseSelect() + """
                ORDER BY pr.created_at DESC, pr.id DESC
                """;
        return jdbcTemplate.query(sql, rowMapper());
    }

    public Optional<ProductReviewResponse> findById(UUID reviewId) {
        String sql = baseSelect() + """
                WHERE pr.id = :reviewId
                """;

        List<ProductReviewResponse> reviews = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("reviewId", reviewId),
                rowMapper()
        );
        return reviews.stream().findFirst();
    }

    public ProductReviewResponse save(UUID productId, UUID userId, String author, String title, String comment, int rating) {
        UUID reviewId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                        INSERT INTO product_reviews (
                            id,
                            product_id,
                            user_id,
                            review_title,
                            review_comment,
                            rating,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            :id,
                            :productId,
                            :userId,
                            :title,
                            :comment,
                            :rating,
                            :createdAt,
                            :updatedAt
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", reviewId)
                        .addValue("productId", productId)
                        .addValue("userId", userId)
                        .addValue("title", title)
                        .addValue("comment", comment)
                        .addValue("rating", rating)
                        .addValue("createdAt", now)
                        .addValue("updatedAt", now)
        );

        return ProductReviewResponse.builder()
                .id(reviewId)
                .productId(productId)
                .userId(userId)
                .author(author)
                .title(title)
                .comment(comment)
                .rating(rating)
                .createdAt(now)
                .build();
    }

    public void deleteById(UUID reviewId) {
        jdbcTemplate.update(
                "DELETE FROM product_reviews WHERE id = :reviewId",
                new MapSqlParameterSource("reviewId", reviewId)
        );
    }

    private String baseSelect() {
        return """
                SELECT
                    pr.id,
                    pr.product_id,
                    pr.user_id,
                    COALESCE(NULLIF(TRIM(u.username), ''), u.email) AS author,
                    pr.review_title,
                    pr.review_comment,
                    pr.rating,
                    pr.created_at
                FROM product_reviews pr
                JOIN users u ON u.id = pr.user_id
                """;
    }

    private RowMapper<ProductReviewResponse> rowMapper() {
        return new RowMapper<>() {
            @Override
            public ProductReviewResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                Timestamp createdAt = rs.getTimestamp("created_at");

                return ProductReviewResponse.builder()
                        .id(rs.getObject("id", UUID.class))
                        .productId(rs.getObject("product_id", UUID.class))
                        .userId(rs.getObject("user_id", UUID.class))
                        .author(rs.getString("author"))
                        .title(rs.getString("review_title"))
                        .comment(rs.getString("review_comment"))
                        .rating(rs.getInt("rating"))
                        .createdAt(createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now())
                        .build();
            }
        };
    }
}
