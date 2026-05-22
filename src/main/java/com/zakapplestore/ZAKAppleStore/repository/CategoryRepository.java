package com.zakapplestore.ZAKAppleStore.repository;

import com.zakapplestore.ZAKAppleStore.dto.CategoryResponse;
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
public class CategoryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<CategoryResponse> CATEGORY_ROW_MAPPER = new RowMapper<>() {
        @Override
        public CategoryResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
            Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");

            return CategoryResponse.builder()
                    .id(rs.getObject("id", UUID.class))
                    .name(rs.getString("name"))
                    .createdAt(createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : LocalDateTime.now())
                    .updatedAt(updatedAtTimestamp != null ? updatedAtTimestamp.toLocalDateTime() : LocalDateTime.now())
                    .build();
        }
    };

    public List<CategoryResponse> findAll() {
        String sql = """
                SELECT id, name, created_at, updated_at
                FROM categories
                ORDER BY created_at DESC
                """;
        return jdbcTemplate.query(sql, CATEGORY_ROW_MAPPER);
    }

    public Optional<CategoryResponse> findById(UUID id) {
        String sql = """
                SELECT id, name, created_at, updated_at
                FROM categories
                WHERE id = :id
                """;
        List<CategoryResponse> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("id", id),
                CATEGORY_ROW_MAPPER
        );
        return rows.stream().findFirst();
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM categories";
        Long count = jdbcTemplate.getJdbcTemplate().queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    public boolean existsByNameIgnoreCase(String name) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM categories
                    WHERE LOWER(name) = LOWER(:name)
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("name", name),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM categories
                    WHERE LOWER(name) = LOWER(:name)
                      AND id <> :id
                )
                """;
        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("name", name)
                        .addValue("id", id),
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    public CategoryResponse save(String name) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        String insertSql = """
                INSERT INTO categories (id, name, created_at, updated_at)
                VALUES (:id, :name, :createdAt, :updatedAt)
                """;
        jdbcTemplate.update(
                insertSql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("name", name)
                        .addValue("createdAt", now)
                        .addValue("updatedAt", now)
        );

        return findById(id).orElseThrow();
    }

    public CategoryResponse update(UUID id, String name) {
        String updateSql = """
                UPDATE categories
                SET name = :name,
                    updated_at = :updatedAt
                WHERE id = :id
                """;
        jdbcTemplate.update(
                updateSql,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("name", name)
                        .addValue("updatedAt", LocalDateTime.now())
        );
        return findById(id).orElseThrow();
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM categories WHERE id = :id";
        jdbcTemplate.update(sql, new MapSqlParameterSource("id", id));
    }
}
