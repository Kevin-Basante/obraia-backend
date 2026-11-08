package com.obraia.backend.util;

import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Values of a sentence (project, city, days...) saved as a JSON object of
 * texts, so the frontend can write the sentence in the language of the user.
 */
public final class TextParams {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {
    };

    private TextParams() {
    }

    /** Null when there are no params, so the column stays empty. */
    public static String toJson(Map<String, String> params) {
        return params == null || params.isEmpty() ? null : JSON.writeValueAsString(params);
    }

    public static Map<String, String> fromJson(String json) {
        return json == null ? null : JSON.readValue(json, MAP);
    }
}
