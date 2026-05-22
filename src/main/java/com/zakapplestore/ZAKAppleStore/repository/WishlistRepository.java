package com.zakapplestore.ZAKAppleStore.repository;

import com.zakapplestore.ZAKAppleStore.dto.WishlistItemResponse;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WishlistRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<WishlistItemResponse> getWishlist(UUID userId) {
        return jdbcTemplate.query(
                """
                        SELECT
                            p.id AS product_id,
                            p.product_code,
                            p.product_name,
                            c.name AS category_name,
                            p.price,
                            p.mrp,
                            p.main_photo,
                            wi.created_at
                        FROM wishlists w
                        JOIN wishlist_items wi ON wi.wishlist_id = w.id
                        JOIN products p ON p.id = wi.product_id
                        JOIN categories c ON c.id = p.category_id
                        WHERE w.user_id = :userId
                        ORDER BY wi.created_at DESC
                        """,
                new MapSqlParameterSource("userId", userId),
                wishlistItemRowMapper()
        );
    }

    public List<WishlistItemResponse> addItem(UUID userId, UUID productId) {
        UUID wishlistId = getOrCreateWishlistId(userId);

        jdbcTemplate.update(
                """
                        INSERT INTO wishlist_items (
                            id,
                            wishlist_id,
                            product_id,
                            created_at
                        )
                        VALUES (
                            :id,
                            :wishlistId,
                            :productId,
                            CURRENT_TIMESTAMP
                        )
                        ON CONFLICT (wishlist_id, product_id) DO NOTHING
                        """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("wishlistId", wishlistId)
                        .addValue("productId", productId)
        );

        return getWishlist(userId);
    }

    public List<WishlistItemResponse> removeItem(UUID userId, UUID productId) {
        int deleted = jdbcTemplate.update(
                """
                        DELETE FROM wishlist_items wi
                        USING wishlists w
                        WHERE wi.wishlist_id = w.id
                          AND w.user_id = :userId
                          AND wi.product_id = :productId
                        """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("productId", productId)
        );

        if (deleted == 0) {
            throw new ResourceNotFoundException("Wishlist item not found");
        }

        return getWishlist(userId);
    }

    private UUID getOrCreateWishlistId(UUID userId) {
        List<UUID> wishlistIds = jdbcTemplate.query(
                """
                        SELECT id
                        FROM wishlists
                        WHERE user_id = :userId
                        """,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );

        if (!wishlistIds.isEmpty()) {
            return wishlistIds.get(0);
        }

        UUID wishlistId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO wishlists (id, user_id, created_at)
                        VALUES (:id, :userId, CURRENT_TIMESTAMP)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", wishlistId)
                        .addValue("userId", userId)
        );
        return wishlistId;
    }

    private RowMapper<WishlistItemResponse> wishlistItemRowMapper() {
        return new RowMapper<>() {
            @Override
            public WishlistItemResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                Timestamp createdAt = rs.getTimestamp("created_at");

                return WishlistItemResponse.builder()
                        .productId(rs.getObject("product_id", UUID.class))
                        .productCode(rs.getString("product_code"))
                        .productName(rs.getString("product_name"))
                        .category(rs.getString("category_name"))
                        .price(defaultAmount(rs.getBigDecimal("price")))
                        .mrp(defaultAmount(rs.getBigDecimal("mrp")))
                        .image(rs.getString("main_photo"))
                        .createdAt(createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now())
                        .build();
            }
        };
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
