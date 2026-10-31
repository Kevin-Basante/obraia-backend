package com.obraia.backend.ai;

import java.util.List;

/**
 * What the AI read in a blueprint.
 *
 * @param discarded elements the AI returned that were not valid
 * @param model     model that read the blueprint
 * @param cached    true when the answer did not need a new call to the AI
 */
public record BlueprintAnalysis(List<AiMeasurement> measurements, List<String> warnings, int discarded,
        String model, boolean cached) {

    public BlueprintAnalysis {
        measurements = List.copyOf(measurements);
        warnings = List.copyOf(warnings);
    }
}
