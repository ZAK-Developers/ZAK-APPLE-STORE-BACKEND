package com.zakapplestore.ZAKAppleStore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
public class CategorySchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (isLegacySchemaDetected()) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS categories CASCADE");
        }

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id UUID PRIMARY KEY,
                    name VARCHAR(40) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_name_lower
                ON categories ((LOWER(name)))
                """);
    }

    private boolean isLegacySchemaDetected() {
        String sql = """
                SELECT
                    EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'categories'
                          AND column_name = 'id'
                          AND udt_name <> 'uuid'
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM (VALUES
                            ('id'),
                            ('name'),
                            ('created_at'),
                            ('updated_at')
                        ) AS required_columns(column_name)
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM information_schema.columns c
                            WHERE c.table_schema = 'public'
                              AND c.table_name = 'categories'
                              AND c.column_name = required_columns.column_name
                        )
                    )
                """;

        Boolean legacySchemaDetected = jdbcTemplate.queryForObject(sql, Boolean.class);
        return Boolean.TRUE.equals(legacySchemaDetected);
    }
}
