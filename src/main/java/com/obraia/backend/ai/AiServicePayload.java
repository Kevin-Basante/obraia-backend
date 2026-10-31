package com.obraia.backend.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Answer of POST /api/v1/blueprints/analyze of obraia-ia, as it comes:
 * Python names in snake_case and plain numbers. Only the adapter uses it.
 */
record AiServicePayload(List<Item> measurements, List<String> warnings, int discarded, String model) {

    record Item(@JsonProperty("element_type") String elementType,
            @JsonProperty("floor_number") int floorNumber,
            @JsonProperty("length_m") double lengthM,
            @JsonProperty("width_m") Double widthM,
            @JsonProperty("height_m") Double heightM,
            int quantity,
            String notes) {
    }
}
