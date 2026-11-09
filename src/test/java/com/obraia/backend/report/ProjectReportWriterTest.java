package com.obraia.backend.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.obraia.backend.support.ReportFixtures;

/** The writers make real PDFs; their text is read back to check what each report shows. */
class ProjectReportWriterTest {

    private final PublicReportWriter publicWriter = new PublicReportWriter();
    private final FullReportWriter fullWriter = new FullReportWriter();

    @Test
    void thePublicReportShowsTheWorkButNoMoneyOrRisk() throws IOException {
        ReportFile file = publicWriter.write(ReportFixtures.publicData(), ReportTexts.of(ReportLanguage.ES));
        String text = text(file);

        assertEquals("casa-basante-public-report.pdf", file.filename());
        assertTrue(text.contains("Informe público"));
        assertTrue(text.contains("Avance de la obra"));
        assertTrue(text.contains("Cimentación"));
        assertTrue(text.contains("36,4%"));
        assertFalse(text.contains("Presupuesto"));
        assertFalse(text.contains("Riesgo"));
        assertFalse(text.contains("Resumen"));
    }

    @Test
    void theFullReportAddsMoneyRiskAndRecords() throws IOException {
        ReportFile file = fullWriter.write(ReportFixtures.fullData(), ReportTexts.of(ReportLanguage.EN));
        String text = text(file);

        assertEquals("casa-basante-full-report.pdf", file.filename());
        assertTrue(text.contains("Budget and expenses"));
        assertTrue(text.contains("$ 68.645.101"));
        assertTrue(text.contains("-$ 11.354.899"));
        assertTrue(text.contains("Critical"));
        assertTrue(text.contains("38 days"));
        assertTrue(text.contains("\"Vigas\" is 38 days late"));
        assertTrue(text.contains("Records"));
    }

    @Test
    void theSummaryOfTheAiGoesWhenThereIsOne() throws IOException {
        ReportData data = ReportFixtures.publicData().withSummary(ReportFixtures.summary());

        String text = text(publicWriter.write(data, ReportTexts.of(ReportLanguage.ES)));

        assertTrue(text.contains("Resumen"));
        assertTrue(text.contains("Reprogramar las tareas atrasadas."));
        assertTrue(text.contains("gemini-test"));
    }

    @Test
    void withoutDatesOfItsOwnTheProjectShowsTheDatesOfThePlan() throws IOException {
        String text = text(publicWriter.write(ReportFixtures.publicData(), ReportTexts.of(ReportLanguage.EN)));

        assertTrue(text.contains("Sep 1, 2026 to Jan 30, 2027"));
    }

    @Test
    void theFileNameHasNoAccentsOrSpaces() {
        assertEquals("cabana-n-1-public-report.pdf", publicWriter.filename("Cabaña Nº 1"));
        assertEquals("project-full-report.pdf", fullWriter.filename("¿?"));
    }

    private static String text(ReportFile file) throws IOException {
        assertTrue(new String(file.content(), 0, 5).startsWith("%PDF"));
        PdfReader reader = new PdfReader(file.content());
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }
}
