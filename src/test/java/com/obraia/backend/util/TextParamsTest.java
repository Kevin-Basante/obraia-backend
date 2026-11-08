package com.obraia.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TextParamsTest {

    @Test
    void paramsGoAndComeBackAsJson() {
        String json = TextParams.toJson(Map.of("project", "Casa Avisos"));

        assertEquals("{\"project\":\"Casa Avisos\"}", json);
        assertEquals(Map.of("project", "Casa Avisos"), TextParams.fromJson(json));
    }

    @Test
    void noParamsLeaveTheColumnEmpty() {
        assertNull(TextParams.toJson(Map.of()));
        assertNull(TextParams.toJson(null));
        assertNull(TextParams.fromJson(null));
    }
}
