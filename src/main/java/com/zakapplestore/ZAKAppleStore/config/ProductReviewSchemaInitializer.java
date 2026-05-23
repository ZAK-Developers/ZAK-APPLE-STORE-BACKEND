package com.zakapplestore.ZAKAppleStore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(25)
@RequiredArgsConstructor
public class ProductReviewSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS product_reviews (
                    id UUID PRIMARY KEY,
                    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
                    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    review_title VARCHAR(180) NOT NULL,
                    review_comment TEXT NOT NULL,
                    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id
                ON product_reviews(product_id)
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_product_reviews_user_id
                ON product_reviews(user_id)
                """);
    }
}
