package com.obraia.backend.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.obraia.backend.config.ClockConfig;

/**
 * Texts and formats of one report. The texts live in
 * report/messages.properties (English) and report/messages_es.properties
 * (Spanish). Dates follow the language; money and numbers keep the
 * Colombian format in both, like the frontend.
 */
public final class ReportTexts {

    private static final String BUNDLE = "report/messages";
    // Without this control, a computer set to Spanish would also get Spanish for English.
    private static final ResourceBundle.Control NO_FALLBACK = ResourceBundle.Control
            .getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final ReportLanguage language;
    private final ResourceBundle bundle;
    private final ResourceBundle english;
    private final DateTimeFormatter dateFormat;
    private final DateTimeFormatter dateTimeFormat;

    private ReportTexts(ReportLanguage language) {
        this.language = language;
        this.english = ResourceBundle.getBundle(BUNDLE, Locale.ROOT, NO_FALLBACK);
        this.bundle = language == ReportLanguage.ES
                ? ResourceBundle.getBundle(BUNDLE, language.getLocale(), NO_FALLBACK)
                : english;
        if (language == ReportLanguage.ES) {
            this.dateFormat = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", COLOMBIA);
            this.dateTimeFormat = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy, h:mm a", COLOMBIA);
        } else {
            this.dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
            this.dateTimeFormat = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.US);
        }
    }

    public static ReportTexts of(ReportLanguage language) {
        return new ReportTexts(language);
    }

    public ReportLanguage language() {
        return language;
    }

    /** The text of a key with its values: "{done} of {total}". A missing key is returned as it is. */
    public String text(String key, Map<String, ?> values) {
        String pattern = find(pluralKey(key, values));
        for (Map.Entry<String, ?> value : values.entrySet()) {
            pattern = pattern.replace("{" + value.getKey() + "}", String.valueOf(value.getValue()));
        }
        return pattern;
    }

    public String text(String key) {
        return text(key, Map.of());
    }

    /** True when the key exists, in the language or in English. */
    public boolean has(String key) {
        return bundle.containsKey(key) || english.containsKey(key);
    }

    /** Label of a code ("status.in_progress"), or the fallback sent by the backend when the code is not known. */
    public String code(String group, String code, String fallback) {
        String key = group + "." + code;
        return code != null && has(key) ? text(key) : fallback;
    }

    /** "key.one" or "key.other" when the key is a plural and a count is given. */
    private String pluralKey(String key, Map<String, ?> values) {
        Object count = values.get("count");
        if (count == null || has(key)) {
            return key;
        }
        return key + ("1".equals(String.valueOf(count)) ? ".one" : ".other");
    }

    private String find(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException missing) {
            try {
                return english.getString(key);
            } catch (MissingResourceException notInEnglish) {
                return key;
            }
        }
    }

    /** 57500000 -> "$ 57.500.000"; a negative amount -> "-$ 1.500" */
    public String money(BigDecimal amount) {
        String value = "$ " + number(amount.abs().setScale(0, RoundingMode.HALF_UP), 0);
        return amount.signum() < 0 ? "-" + value : value;
    }

    /** 20.54 -> "20,5%" */
    public String percent(BigDecimal value) {
        return number(value, 1) + "%";
    }

    /** 96.5 -> "96,5", with up to the given decimals and the dot for thousands. */
    public String number(BigDecimal value, int decimals) {
        String pattern = decimals == 0 ? "#,##0" : "#,##0." + "#".repeat(decimals);
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(COLOMBIA));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    /** "Nov 9, 2026" or "9 de nov de 2026". */
    public String date(LocalDate date) {
        return withoutDots(dateFormat.format(date));
    }

    /** "Nov 9, 2026, 6:10 PM" or "9 de nov de 2026, 6:10 p. m.", in the time of Colombia. */
    public String dateTime(Instant instant) {
        return withoutDots(dateTimeFormat.format(instant.atZone(ClockConfig.ZONE)));
    }

    /** "Nov 3, 2026 to Dec 1, 2026"; without dates it says so. */
    public String dateRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return text("facts.noDates");
        }
        return text("dates.range", Map.of("start", date(start), "end", date(end)));
    }

    /** Java writes the Spanish months with a dot ("nov."); the frontend does not, so the reports do not either. */
    private String withoutDots(String text) {
        return language == ReportLanguage.ES ? text.replaceAll("(\\p{L}{3,4})\\. ", "$1 ") : text;
    }
}
