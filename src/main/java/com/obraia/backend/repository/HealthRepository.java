package com.obraia.backend.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Database queries used by the health check (Repository pattern):
 * the only class in this flow that writes SQL.
 */
@Repository
public class HealthRepository {

    private final JdbcTemplate jdbcTemplate;

    public HealthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Runs the lightest possible query to check that the database answers. */
    public void ping() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
    }

    /** Counts the application tables, without the Flyway history table. */
    public int countTables() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' "
                        + "AND table_type = 'BASE TABLE' "
                        + "AND table_name <> 'flyway_schema_history'",
                Integer.class);
        return tables == null ? 0 : tables;
    }

    /** Reads the project type names loaded by the seed migration. */
    public List<String> findProjectTypeNames() {
        return jdbcTemplate.queryForList("SELECT name FROM project_types ORDER BY name", String.class);
    }
}
