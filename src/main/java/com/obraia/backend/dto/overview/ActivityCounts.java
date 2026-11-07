package com.obraia.backend.dto.overview;

/** How much was recorded in the project. */
public record ActivityCounts(long blueprints, long manualMeasurements, long aiMeasurements, long members) {
}
