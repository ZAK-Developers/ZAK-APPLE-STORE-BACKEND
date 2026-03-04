package com.zakapplestore.ZAKAppleStore.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConnectionLogger implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        String schema = jdbcTemplate.queryForObject("select current_schema()", String.class);
        String version = jdbcTemplate.queryForObject("select version()", String.class);

        log.info("Connected PostgreSQL database='{}', schema='{}'", database, schema);
        if (version != null) {
            log.info("PostgreSQL version: {}", version);
        }
    }
}
