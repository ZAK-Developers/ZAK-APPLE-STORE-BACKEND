package com.zakapplestore.ZAKAppleStore.repository;

import com.zakapplestore.ZAKAppleStore.dto.CartItemResponse;
import com.zakapplestore.ZAKAppleStore.dto.CartMergeItemRequest;
import com.zakapplestore.ZAKAppleStore.dto.CartResponse;
import com.zakapplestore.ZAKAppleStore.exception.BadRequestException;
import com.zakapplestore.ZAKAppleStore.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CartRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CartResponse getCart(UUID userId) {
        List<CartItemResponse> items = findCartItems(userId);
        BigDecimal subtotal = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tax = BigDecimal.ZERO;

        return CartResponse.builder()
                .items(items)
                .subtotal(subtotal)
                .tax(tax)
                .total(subtotal.add(tax))
                .build();
    }

    public CartResponse addItem(UUID userId, UUID productId, int quantity, String color, String storage) {
        UUID cartId = getOrCreateCartId(userId);
        ProductSnapshot product = requireProduct(productId);
        int normalizedQuantity = Math.max(1, quantity);

        if (product.stockQuantity() < normalizedQuantity) {
            throw new BadRequestException("Requested quantity is not available in stock.");
        }

        List<UUID> existing = jdbcTemplate.query(
                """
                        SELECT id
                        FROM cart_items
                        WHERE cart_id = :cartId
                          AND product_id = :productId
                          AND LOWER(color) = LOWER(:color)
                          AND LOWER(storage) = LOWER(:storage)
                        """,
                new MapSqlParameterSource()
                        .addValue("cartId", cartId)
                        .addValue("productId", productId)
                        .addValue("color", color)
                        .addValue("storage", storage),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );

        if (!existing.isEmpty()) {
            throw new BadRequestException("Product is already in the cart.");
        }

        jdbcTemplate.update(
                """
                        INSERT INTO cart_items (
                            id,
                            cart_id,
                            product_id,
                            color,
                            storage,
                            quantity,
                            created_at,
                            updated_at
                        )
                        VALUES (
                            :id,
                            :cartId,
                            :productId,
                            :color,
                            :storage,
                            :quantity,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("cartId", cartId)
                        .addValue("productId", productId)
                        .addValue("color", color.trim())
                        .addValue("storage", storage.trim())
                        .addValue("quantity", normalizedQuantity)
        );

        return getCart(userId);
    }

    public CartResponse updateQuantity(UUID userId, UUID cartItemId, int quantity) {
        ProductSnapshot product = requireCartItemProduct(userId, cartItemId);
        int normalizedQuantity = Math.max(1, quantity);

        if (product.stockQuantity() < normalizedQuantity) {
            throw new BadRequestException("Requested quantity is not available in stock.");
        }

        jdbcTemplate.update(
                """
                        UPDATE cart_items
                        SET quantity = :quantity,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :cartItemId
                        """,
                new MapSqlParameterSource()
                        .addValue("cartItemId", cartItemId)
                        .addValue("quantity", normalizedQuantity)
        );

        return getCart(userId);
    }

    public CartResponse removeItem(UUID userId, UUID cartItemId) {
        int deleted = jdbcTemplate.update(
                """
                        DELETE FROM cart_items ci
                        USING carts c
                        WHERE ci.id = :cartItemId
                          AND ci.cart_id = c.id
                          AND c.user_id = :userId
                        """,
                new MapSqlParameterSource()
                        .addValue("cartItemId", cartItemId)
                        .addValue("userId", userId)
        );

        if (deleted == 0) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        return getCart(userId);
    }

    public void clearCart(UUID userId) {
        jdbcTemplate.update(
                """
                        DELETE FROM cart_items ci
                        USING carts c
                        WHERE ci.cart_id = c.id
                          AND c.user_id = :userId
                        """,
                new MapSqlParameterSource("userId", userId)
        );
    }

    public CartResponse mergeItems(UUID userId, List<CartMergeItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return getCart(userId);
        }

        UUID cartId = getOrCreateCartId(userId);

        for (CartMergeItemRequest item : items) {
            ProductSnapshot product = requireProduct(item.getProductId());
            int normalizedQuantity = Math.max(1, item.getQuantity());

            List<CartItemRow> existingItems = jdbcTemplate.query(
                    """
                            SELECT id, quantity
                            FROM cart_items
                            WHERE cart_id = :cartId
                              AND product_id = :productId
                              AND LOWER(color) = LOWER(:color)
                              AND LOWER(storage) = LOWER(:storage)
                            """,
                    new MapSqlParameterSource()
                            .addValue("cartId", cartId)
                            .addValue("productId", item.getProductId())
                            .addValue("color", item.getColor())
                            .addValue("storage", item.getStorage()),
                    (rs, rowNum) -> new CartItemRow(
                            rs.getObject("id", UUID.class),
                            rs.getInt("quantity")
                    )
            );

            if (!existingItems.isEmpty()) {
                CartItemRow existing = existingItems.get(0);
                int mergedQuantity = Math.max(1, Math.min(product.stockQuantity(), existing.quantity() + normalizedQuantity));

                jdbcTemplate.update(
                        """
                                UPDATE cart_items
                                SET quantity = :quantity,
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE id = :id
                                """,
                        new MapSqlParameterSource()
                                .addValue("id", existing.id())
                                .addValue("quantity", mergedQuantity)
                );
                continue;
            }

            normalizedQuantity = Math.min(product.stockQuantity(), normalizedQuantity);
            if (normalizedQuantity <= 0) {
                continue;
            }

            jdbcTemplate.update(
                    """
                            INSERT INTO cart_items (
                                id,
                                cart_id,
                                product_id,
                                color,
                                storage,
                                quantity,
                                created_at,
                                updated_at
                            )
                            VALUES (
                                :id,
                                :cartId,
                                :productId,
                                :color,
                                :storage,
                                :quantity,
                                CURRENT_TIMESTAMP,
                                CURRENT_TIMESTAMP
                            )
                            """,
                    new MapSqlParameterSource()
                            .addValue("id", UUID.randomUUID())
                            .addValue("cartId", cartId)
                            .addValue("productId", item.getProductId())
                            .addValue("color", item.getColor().trim())
                            .addValue("storage", item.getStorage().trim())
                            .addValue("quantity", normalizedQuantity)
            );
        }

        return getCart(userId);
    }

    private UUID getOrCreateCartId(UUID userId) {
        List<UUID> cartIds = jdbcTemplate.query(
                """
                        SELECT id
                        FROM carts
                        WHERE user_id = :userId
                        """,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );

        if (!cartIds.isEmpty()) {
            return cartIds.get(0);
        }

        UUID cartId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO carts (id, user_id, created_at)
                        VALUES (:id, :userId, CURRENT_TIMESTAMP)
                        """,
                new MapSqlParameterSource()
                        .addValue("id", cartId)
                        .addValue("userId", userId)
        );
        return cartId;
    }

    private List<CartItemResponse> findCartItems(UUID userId) {
        return jdbcTemplate.query(
                """
                        SELECT
                            ci.id AS cart_item_id,
                            p.id AS product_id,
                            p.product_code,
                            p.product_name,
                            c.name AS category_name,
                            ci.quantity,
                            p.price,
                            p.mrp,
                            p.main_photo,
                            ci.color,
                            ci.storage
                        FROM carts cart
                        JOIN cart_items ci ON ci.cart_id = cart.id
                        JOIN products p ON p.id = ci.product_id
                        JOIN categories c ON c.id = p.category_id
                        WHERE cart.user_id = :userId
                        ORDER BY ci.created_at DESC
                        """,
                new MapSqlParameterSource("userId", userId),
                cartItemRowMapper()
        );
    }

    private ProductSnapshot requireProduct(UUID productId) {
        List<ProductSnapshot> products = jdbcTemplate.query(
                """
                        SELECT id, stock_quantity, status
                        FROM products
                        WHERE id = :productId
                        """,
                new MapSqlParameterSource("productId", productId),
                (rs, rowNum) -> new ProductSnapshot(
                        rs.getObject("id", UUID.class),
                        rs.getInt("stock_quantity"),
                        rs.getString("status")
                )
        );

        if (products.isEmpty()) {
            throw new ResourceNotFoundException("Product not found");
        }

        ProductSnapshot product = products.get(0);
        if (!"Active".equalsIgnoreCase(product.status())) {
            throw new BadRequestException("This product is not available for purchase.");
        }
        return product;
    }

    private ProductSnapshot requireCartItemProduct(UUID userId, UUID cartItemId) {
        List<ProductSnapshot> products = jdbcTemplate.query(
                """
                        SELECT p.id, p.stock_quantity, p.status
                        FROM cart_items ci
                        JOIN carts cart ON cart.id = ci.cart_id
                        JOIN products p ON p.id = ci.product_id
                        WHERE ci.id = :cartItemId
                          AND cart.user_id = :userId
                        """,
                new MapSqlParameterSource()
                        .addValue("cartItemId", cartItemId)
                        .addValue("userId", userId),
                (rs, rowNum) -> new ProductSnapshot(
                        rs.getObject("id", UUID.class),
                        rs.getInt("stock_quantity"),
                        rs.getString("status")
                )
        );

        if (products.isEmpty()) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        ProductSnapshot product = products.get(0);
        if (!"Active".equalsIgnoreCase(product.status())) {
            throw new BadRequestException("This product is not available for purchase.");
        }
        return product;
    }

    private RowMapper<CartItemResponse> cartItemRowMapper() {
        return new RowMapper<>() {
            @Override
            public CartItemResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
                return CartItemResponse.builder()
                        .id(rs.getObject("cart_item_id", UUID.class))
                        .productId(rs.getObject("product_id", UUID.class))
                        .productCode(rs.getString("product_code"))
                        .productName(rs.getString("product_name"))
                        .category(rs.getString("category_name"))
                        .quantity(rs.getInt("quantity"))
                        .price(defaultAmount(rs.getBigDecimal("price")))
                        .mrp(defaultAmount(rs.getBigDecimal("mrp")))
                        .image(rs.getString("main_photo"))
                        .color(rs.getString("color"))
                        .storage(rs.getString("storage"))
                        .build();
            }
        };
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record ProductSnapshot(UUID id, int stockQuantity, String status) {}

    private record CartItemRow(UUID id, int quantity) {}
}
