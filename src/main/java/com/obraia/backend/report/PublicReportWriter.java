package com.obraia.backend.report;

import org.springframework.stereotype.Component;

/** The public report: only the steps of the template, without money or risk. */
@Component
public class PublicReportWriter extends ProjectReportWriter {

    @Override
    public ReportType type() {
        return ReportType.PUBLIC;
    }

    @Override
    protected void writeRestrictedSections(ReportDocument document, ReportData data, ReportTexts texts) {
        // The public report can be shared with anyone, so it adds nothing.
    }
}
