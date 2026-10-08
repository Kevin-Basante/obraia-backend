package com.obraia.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response of GET /api/hello: the "Hello world" message plus proof that the
 * backend read real data from the database.
 */
public record HelloResponse(
        String message,
        String project,
        Database database,
        OffsetDateTime timestamp) {

    /**
     * @param status       always "connected" (if the database fails, the endpoint returns an error)
     * @param tables       number of application tables
     * @param projectTypes names read from the project_types table
     * @param latencyMs    time the queries took
     */
    public record Database(
            String status,
            int tables,
            List<String> projectTypes,
            long latencyMs) {
    }
}
