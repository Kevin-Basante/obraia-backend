package com.obraia.backend.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Counts what a project holds in the other modules, so the user sees
 * what will be lost before deleting it. The tables already exist (V2)
 * even though their screens come in later blocks.
 */
@Repository
public class ProjectContentRepository {

    // Key shown to the client and table to count. Fixed names, never user input.
    private static final Map<String, String> TABLES = new LinkedHashMap<>();

    static {
        TABLES.put("blueprints", "blueprints");
        TABLES.put("phases", "phases");
        TABLES.put("budgets", "budgets");
        TABLES.put("expenses", "expenses");
        TABLES.put("members", "project_members");
    }

    private final JdbcTemplate jdbcTemplate;

    public ProjectContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Integer> countContent(UUID projectId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        TABLES.forEach((key, table) -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + table + " WHERE project_id = ?", Integer.class, projectId);
            counts.put(key, count == null ? 0 : count);
        });
        return counts;
    }
}
