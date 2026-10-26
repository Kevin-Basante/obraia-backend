package com.obraia.backend.support;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.blueprint.Blueprint;
import com.obraia.backend.entity.blueprint.BlueprintFileType;
import com.obraia.backend.entity.blueprint.BlueprintMeasurement;
import com.obraia.backend.entity.blueprint.MeasurementSource;
import com.obraia.backend.entity.blueprint.StorageProvider;
import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.project.Project;

/** Builds blueprints and measurements for unit tests. Ids and database dates are set by reflection. */
public final class BlueprintFixtures {

    /** The smallest valid PNG header; enough for the type detector. */
    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

    private BlueprintFixtures() {
    }

    public static Blueprint blueprint(Project project, String filename) {
        Blueprint blueprint = new Blueprint(project, project.getCreatedBy(), "projects/" + project.getId() + "/a.png",
                filename, BlueprintFileType.IMAGE, "image/png", PNG.length, StorageProvider.EXTERNAL);
        set(blueprint, "id", UUID.randomUUID());
        set(blueprint, "uploadedAt", Instant.parse("2026-10-26T15:00:00Z"));
        return blueprint;
    }

    public static BlueprintMeasurement measurement(Project project, MeasurementSource source, ElementType element,
            int floor, String lengthM, String widthM, String heightM, int quantity) {
        BlueprintMeasurement measurement = new BlueprintMeasurement(project, source);
        measurement.update(null, element, floor, decimal(lengthM), decimal(widthM), decimal(heightM), quantity,
                null);
        set(measurement, "id", UUID.randomUUID());
        set(measurement, "createdAt", Instant.parse("2026-10-26T15:00:00Z"));
        return measurement;
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
