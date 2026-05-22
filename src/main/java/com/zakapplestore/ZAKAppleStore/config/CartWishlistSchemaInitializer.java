package com.zakapplestore.ZAKAppleStore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
public class CartWishlistSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS carts (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cart_items (
                    id UUID PRIMARY KEY,
                    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
                    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                    color VARCHAR(60) NOT NULL,
                    storage VARCHAR(60) NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_cart_item_variant UNIQUE (cart_id, product_id, color, storage)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wishlists (
                    id UUID PRIMARY KEY,
                    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wishlist_items (
                    id UUID PRIMARY KEY,
                    wishlist_id UUID NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
                    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_wishlist_product UNIQUE (wishlist_id, product_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_carts_user_id
                ON carts(user_id)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id
                ON cart_items(cart_id)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_cart_items_product_id
                ON cart_items(product_id)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_wishlists_user_id
                ON wishlists(user_id)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_wishlist_items_wishlist_id
                ON wishlist_items(wishlist_id)
                """);
    }
}
