package com.obraia.backend.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Text comparison for search boxes: ignores upper case and accents, so
 * "rio" finds "Arena de río" and "bogota" finds "Bogotá".
 */
public final class TextSearch {

    private TextSearch() {
    }

    public static String normalize(String text) {
        // NFD splits "í" into "i" plus an accent mark, and the regex removes the marks.
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }

    /** True when the query is empty or appears inside the text. */
    public static boolean matches(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return normalize(text).contains(normalize(query));
    }
}
