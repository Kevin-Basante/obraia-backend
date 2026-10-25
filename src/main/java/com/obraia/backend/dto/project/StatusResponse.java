package com.obraia.backend.dto.project;

import com.obraia.backend.entity.project.ProjectStatus;

/** A status code with the text to show for it. */
public record StatusResponse(String code, String label) {

    public static StatusResponse from(ProjectStatus status) {
        return new StatusResponse(status.getCode(), status.getLabel());
    }
}
