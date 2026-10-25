package com.obraia.backend.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.project.ProjectMember;
import com.obraia.backend.entity.project.ProjectStatus;

/** Builds projects and members for unit tests. Ids and database dates are set by reflection. */
public final class ProjectFixtures {

    private ProjectFixtures() {
    }

    public static Project project(UUID createdBy, String name) {
        Project project = new Project(createdBy, CatalogFixtures.projectType("Vivienda unifamiliar"),
                CatalogFixtures.location("Cali", "Valle del Cauca"), name, null, new BigDecimal("90.00"), 1, null,
                null);
        set(project, "id", UUID.randomUUID());
        set(project, "createdAt", Instant.parse("2026-10-20T15:00:00Z"));
        set(project, "updatedAt", Instant.parse("2026-10-20T15:00:00Z"));
        return project;
    }

    public static Project project(UUID createdBy, String name, ProjectStatus status) {
        Project project = project(createdBy, name);
        set(project, "status", status);
        return project;
    }

    public static Role ownerRole() {
        try {
            Constructor<Role> constructor = Role.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Role role = constructor.newInstance();
            set(role, "id", UUID.randomUUID());
            set(role, "name", Role.OWNER);
            return role;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static ProjectMember member(Project project, User user) {
        return new ProjectMember(project, user, ownerRole());
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
