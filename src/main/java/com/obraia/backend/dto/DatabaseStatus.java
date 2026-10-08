package com.obraia.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Connection state of the database.
 *
 * @param status    "connected" or "disconnected"
 * @param latencyMs time the database took to answer, or null if it did not answer
 */
public record DatabaseStatus(String status, Long latencyMs) {

    public static DatabaseStatus connected(long latencyMs) {
        return new DatabaseStatus("connected", latencyMs);
    }

    public static DatabaseStatus disconnected() {
        return new DatabaseStatus("disconnected", null);
    }

    @JsonIgnore
    public boolean isConnected() {
        return "connected".equals(status);
    }
}
