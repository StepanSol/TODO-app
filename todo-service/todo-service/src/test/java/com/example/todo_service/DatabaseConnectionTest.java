package com.example.todo_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
public class DatabaseConnectionTest extends TestContainersConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
        System.out.println("✅ Successfully connected to PostgreSQL in Docker!");
    }

    @Test
    void testDatabaseVersion() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        System.out.println("PostgreSQL version: " + version);
        assertThat(version).contains("PostgreSQL");
    }
}
