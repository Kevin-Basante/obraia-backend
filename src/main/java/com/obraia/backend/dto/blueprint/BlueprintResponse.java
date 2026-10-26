package com.obraia.backend.dto.blueprint;

import java.time.Instant;
import java.util.UUID;

import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.blueprint.Blueprint;

/** Data of an uploaded blueprint. The file itself is downloaded from its own route. */
public record BlueprintResponse(UUID id, String originalFilename, String fileType, String fileTypeLabel,
        String mimeType, long sizeBytes, StatusResponse processingStatus, Instant uploadedAt) {

    public static BlueprintResponse from(Blueprint blueprint) {
        return new BlueprintResponse(blueprint.getId(), blueprint.getOriginalFilename(),
                blueprint.getFileType().getCode(), blueprint.getFileType().getLabel(), blueprint.getMimeType(),
                blueprint.getSizeBytes(), new StatusResponse(blueprint.getProcessingStatus().getCode(),
                        blueprint.getProcessingStatus().getLabel()),
                blueprint.getUploadedAt());
    }
}
