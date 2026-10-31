package com.obraia.backend.ai;

import java.util.List;

/**
 * What the AI read in a blueprint.
 *
 * @param discarded elements the AI returned that were not valid
 * @param model     model that read the blueprint
 * @param cached    true when the answer came from the cache of the proxy, without calling the AI
 */
public record BlueprintAnalysis(List<AiMeasurement> measurements, List<String> warnings, int discarded,
        String model, boolean cached) {

    public BlueprintAnalysis {
        measurements = List.copyOf(measurements);
        warnings = List.copyOf(warnings);
    }

    public BlueprintAnalysis asCached() {
        return new BlueprintAnalysis(measurements, warnings, discarded, model, true);
    }
}
