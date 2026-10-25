package com.obraia.backend.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.catalog.ElementType;
import com.obraia.backend.entity.catalog.Location;
import com.obraia.backend.entity.catalog.Material;
import com.obraia.backend.entity.catalog.MaterialCategory;
import com.obraia.backend.entity.catalog.MaterialFormula;
import com.obraia.backend.entity.catalog.PhaseTemplate;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.catalog.SizeCategory;

/**
 * Builds catalog rows for unit tests. The catalog entities are read only
 * (no setters), so the fields are filled by reflection, like JPA does.
 */
public final class CatalogFixtures {

    private CatalogFixtures() {
    }

    public static ProjectType projectType(String name) {
        ProjectType type = create(ProjectType.class);
        set(type, "id", UUID.randomUUID());
        set(type, "name", name);
        set(type, "description", "Description of " + name);
        return type;
    }

    public static Location location(String city, String department) {
        Location location = create(Location.class);
        set(location, "id", UUID.randomUUID());
        set(location, "city", city);
        set(location, "department", department);
        set(location, "country", "Colombia");
        return location;
    }

    public static Material material(String name, MaterialCategory category, String price) {
        Material material = create(Material.class);
        set(material, "id", UUID.randomUUID());
        set(material, "name", name);
        set(material, "category", category);
        set(material, "unit", "unidad");
        set(material, "referencePrice", new BigDecimal(price));
        set(material, "priceUpdatedAt", LocalDate.of(2026, 10, 4));
        return material;
    }

    public static PhaseTemplate phase(ProjectType type, SizeCategory size, int order, String name, int days) {
        PhaseTemplate phase = create(PhaseTemplate.class);
        set(phase, "id", UUID.randomUUID());
        set(phase, "projectType", type);
        set(phase, "sizeCategory", size);
        set(phase, "sequenceOrder", order);
        set(phase, "name", name);
        set(phase, "estimatedDurationDays", days);
        return phase;
    }

    public static MaterialFormula formula(ProjectType type, Material material, ElementType element, String consumption) {
        MaterialFormula formula = create(MaterialFormula.class);
        set(formula, "id", UUID.randomUUID());
        set(formula, "projectType", type);
        set(formula, "material", material);
        set(formula, "elementType", element);
        set(formula, "consumptionPerUnit", new BigDecimal(consumption));
        set(formula, "baseUnit", element == ElementType.WALL ? "m2" : "m3");
        set(formula, "wasteFactor", new BigDecimal("0.05"));
        return formula;
    }

    private static <T> T create(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
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
