package com.obraia.backend.report;

import java.util.Locale;

/** Languages of the reports; the frontend sends the one the user picked. */
public enum ReportLanguage {

    EN("en", Locale.ENGLISH),
    ES("es", Locale.forLanguageTag("es-CO"));

    private final String code;
    private final Locale locale;

    ReportLanguage(String code, Locale locale) {
        this.code = code;
        this.locale = locale;
    }

    public String getCode() {
        return code;
    }

    public Locale getLocale() {
        return locale;
    }

    /** "es" (or "es-CO") is Spanish; anything else, or nothing, is English. */
    public static ReportLanguage fromCode(String code) {
        return code != null && code.toLowerCase(Locale.ROOT).startsWith("es") ? ES : EN;
    }
}
