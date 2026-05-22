package com.zakapplestore.ZAKAppleStore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class ProductSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("""
                    CREATE EXTENSION IF NOT EXISTS pg_trgm
                    """);
        } catch (Exception ignored) {
            // Search still works with LIKE matching if trigram extension is unavailable.
        }

        if (isLegacySchemaDetected()) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS products CASCADE");
        }

        jdbcTemplate.execute("""
                CREATE SEQUENCE IF NOT EXISTS products_code_seq START WITH 1 INCREMENT BY 1
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id UUID PRIMARY KEY,
                    product_code VARCHAR(20) UNIQUE NOT NULL,
                    category_id UUID NOT NULL REFERENCES categories(id),
                    product_name VARCHAR(80) NOT NULL,
                    product_description VARCHAR(500) NOT NULL,
                    mrp NUMERIC(12, 2) NOT NULL,
                    price NUMERIC(12, 2) NOT NULL,
                    main_photo TEXT NOT NULL,
                    photo_gallery_json TEXT NOT NULL DEFAULT '[]',
                    stock_quantity INTEGER NOT NULL DEFAULT 0,
                    status VARCHAR(20) NOT NULL DEFAULT 'Active',
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_products_category_id
                ON products(category_id)
                """);

        try {
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_products_name_trgm
                    ON products
                    USING GIN (LOWER(product_name) gin_trgm_ops)
                    """);

            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_products_description_trgm
                    ON products
                    USING GIN (LOWER(product_description) gin_trgm_ops)
                    """);
        } catch (Exception ignored) {
            // Optional trigram indexes only improve search speed when the extension is available.
        }
    }

    private boolean isLegacySchemaDetected() {
        String sql = """
                SELECT
                    EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'products'
                          AND column_name = 'id'
                          AND udt_name <> 'uuid'
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM (VALUES
                            ('id'),
                            ('product_code'),
                            ('category_id'),
                            ('product_name'),
                            ('product_description'),
                            ('mrp'),
                            ('price'),
                            ('main_photo'),
                            ('photo_gallery_json'),
                            ('stock_quantity'),
                            ('status'),
                            ('created_at'),
                            ('updated_at')
                        ) AS required_columns(column_name)
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM information_schema.columns c
                            WHERE c.table_schema = 'public'
                              AND c.table_name = 'products'
                              AND c.column_name = required_columns.column_name
                        )
                    )
                """;

        Boolean legacySchemaDetected = jdbcTemplate.queryForObject(sql, Boolean.class);
        return Boolean.TRUE.equals(legacySchemaDetected);
    }
}
