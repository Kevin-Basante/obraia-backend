package com.obraia.backend.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReportTextsTest {

    private final ReportTexts english = ReportTexts.of(ReportLanguage.EN);
    private final ReportTexts spanish = ReportTexts.of(ReportLanguage.ES);

    @Test
    void readsTheTextsOfEachLanguage() {
        assertEquals("Project report", english.text("report.public.title"));
        assertEquals("Informe de la obra", spanish.text("report.public.title"));
        assertEquals("3 de 22", spanish.text("work.tasksValue", Map.of("done", 3, "total", 22)));
    }

    @Test
    void englishIsNotReplacedBySpanishOnASpanishComputer() {
        Locale before = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("es-CO"));
            assertEquals("Project report", ReportTexts.of(ReportLanguage.EN).text("report.public.title"));
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void picksTheSingularOrThePluralByTheCount() {
        assertEquals("1 día", spanish.text("risk.delayValue", Map.of("count", 1)));
        assertEquals("38 days", english.text("risk.delayValue", Map.of("count", 38)));
    }

    @Test
    void aKeyMissingInSpanishFallsBackToEnglishAndAnUnknownKeyIsKept() {
        assertEquals("ObraIA", spanish.text("report.brand"));
        assertEquals("no.such.key", spanish.text("no.such.key"));
        assertFalse(spanish.has("no.such.key"));
    }

    @Test
    void codesUseTheirLabelOrTheFallback() {
        assertEquals("En curso", spanish.code("status", "in_progress", "In progress"));
        assertEquals("Something new", spanish.code("status", "something_new", "Something new"));
        assertEquals("Fallback", spanish.code("status", null, "Fallback"));
    }

    @Test
    void moneyAndNumbersKeepTheColombianFormatInBothLanguages() {
        assertEquals("$ 68.645.101", english.money(new BigDecimal("68645100.60")));
        assertEquals("-$ 201.654.899", spanish.money(new BigDecimal("-201654899")));
        assertEquals("36,4%", english.percent(new BigDecimal("36.40")));
        assertEquals("100%", spanish.percent(new BigDecimal("100.00")));
        assertEquals("1.234,57", spanish.number(new BigDecimal("1234.567"), 2));
    }

    @Test
    void datesFollowTheLanguage() {
        LocalDate date = LocalDate.of(2026, 11, 9);
        assertEquals("Nov 9, 2026", english.date(date));
        assertEquals("9 de nov de 2026", spanish.date(date));
        assertTrue(spanish.dateTime(Instant.parse("2026-11-09T15:30:00Z")).startsWith("9 de nov de 2026, 10:30"));
        assertEquals("Nov 9, 2026 to Dec 1, 2026", english.dateRange(date, LocalDate.of(2026, 12, 1)));
        assertEquals("Todavía sin fechas", spanish.dateRange(null, date));
    }

    @Test
    void languageAndTypeComeFromTheirCodes() {
        assertEquals(ReportLanguage.ES, ReportLanguage.fromCode("es-CO"));
        assertEquals(ReportLanguage.EN, ReportLanguage.fromCode(null));
        assertEquals(ReportLanguage.EN, ReportLanguage.fromCode("fr"));
        assertEquals(ReportType.FULL, ReportType.fromCode("full"));
    }
}
