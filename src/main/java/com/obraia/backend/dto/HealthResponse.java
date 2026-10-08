package com.obraia.backend.dto;

import java.time.OffsetDateTime;

/**
 * Response of GET /api/health.
 *
 * @param status        "ok" when everything works, "degraded" when the database does not answer
 * @param uptimeSeconds seconds since the server started
 * @param database      database connection state
 * @param timestamp     server date and time
 */
public record HealthResponse(
        String status,
        long uptimeSeconds,
        DatabaseStatus database,
        OffsetDateTime timestamp) {
}
