package com.obraia.backend.dto.blueprint;

import java.util.List;
import java.util.UUID;

import com.obraia.backend.ai.BlueprintAnalysis;
import com.obraia.backend.dto.project.StatusResponse;
import com.obraia.backend.entity.blueprint.Blueprint;

/**
 * Result of reading a blueprint with the AI.
 *
 * @param cached    true when the same file was already read and the AI was not called again
 * @param discarded elements the AI found but could not be saved (unknown type, missing
 *                  dimension or a floor the project does not have)
 * @param warnings  what the AI could not read, in Spanish
 */
public record BlueprintAnalysisResponse(UUID blueprintId, StatusResponse processingStatus, String model,
        boolean cached, int discarded, List<String> warnings, List<MeasurementResponse> measurements) {

    public static BlueprintAnalysisResponse from(Blueprint blueprint, BlueprintAnalysis analysis, int discarded,
            List<MeasurementResponse> measurements) {
        return new BlueprintAnalysisResponse(blueprint.getId(),
                new StatusResponse(blueprint.getProcessingStatus().getCode(),
                        blueprint.getProcessingStatus().getLabel()),
                analysis.model(), analysis.cached(), discarded, analysis.warnings(), measurements);
    }
}
