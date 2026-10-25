package com.obraia.backend.dto.catalog;

import java.util.UUID;

import com.obraia.backend.entity.catalog.ProjectType;

public record ProjectTypeResponse(UUID id, String name, String description) {

    public static ProjectTypeResponse from(ProjectType type) {
        return new ProjectTypeResponse(type.getId(), type.getName(), type.getDescription());
    }
}
