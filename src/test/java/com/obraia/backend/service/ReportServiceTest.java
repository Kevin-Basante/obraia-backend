package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ForbiddenException;
import com.obraia.backend.report.FullReportWriter;
import com.obraia.backend.report.PublicReportWriter;
import com.obraia.backend.report.ReportData;
import com.obraia.backend.report.ReportFile;
import com.obraia.backend.report.ReportLanguage;
import com.obraia.backend.report.ReportSummary;
import com.obraia.backend.report.ReportTexts;
import com.obraia.backend.report.ReportType;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.ReportFixtures;

class ReportServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Prueba");
    private final ReportFile publicFile = new ReportFile("public.pdf", new byte[] { 1 });
    private final ReportFile fullFile = new ReportFile("full.pdf", new byte[] { 2 });

    private ProjectAccessService access;
    private ReportRequestService requests;
    private ReportShareService shares;
    private ReportDataService data;
    private ReportSummaryService summaries;
    private PublicReportWriter publicWriter;
    private FullReportWriter fullWriter;
    private ReportService service;

    @BeforeEach
    void setUp() {
        access = mock(ProjectAccessService.class);
        requests = mock(ReportRequestService.class);
        shares = mock(ReportShareService.class);
        data = mock(ReportDataService.class);
        summaries = mock(ReportSummaryService.class);
        publicWriter = mock(PublicReportWriter.class);
        fullWriter = mock(FullReportWriter.class);
        when(publicWriter.type()).thenReturn(ReportType.PUBLIC);
        when(fullWriter.type()).thenReturn(ReportType.FULL);
        when(publicWriter.write(any(), any())).thenReturn(publicFile);
        when(fullWriter.write(any(), any())).thenReturn(fullFile);
        when(data.collect(project.getId(), ReportType.PUBLIC)).thenReturn(ReportFixtures.publicData());
        when(data.collect(project.getId(), ReportType.FULL)).thenReturn(ReportFixtures.fullData());
        when(access.requireProject(project.getId(), userId)).thenReturn(project);
        service = new ReportService(access, requests, shares, data, summaries, List.of(publicWriter, fullWriter));
    }

    @Test
    void aMemberGetsThePublicReportWithTheSummaryInTheLanguage() {
        ReportSummary summary = ReportFixtures.summary();
        when(summaries.summarize(any(), eq(ReportLanguage.ES))).thenReturn(summary);

        assertSame(publicFile, service.publicReport(project.getId(), userId, "es"));

        verify(access).requireMembership(project.getId(), userId);
        ArgumentCaptor<ReportData> written = ArgumentCaptor.forClass(ReportData.class);
        ArgumentCaptor<ReportTexts> texts = ArgumentCaptor.forClass(ReportTexts.class);
        verify(publicWriter).write(written.capture(), texts.capture());
        assertSame(summary, written.getValue().summary());
        assertEquals(ReportLanguage.ES, texts.getValue().language());
    }

    @Test
    void theFullReportNeedsTheApproval() {
        when(requests.canDownloadFull(project, userId)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.fullReport(project.getId(), userId, "en"));

        assertEquals("REPORT_NOT_APPROVED", ex.getCode());
        verify(data, never()).collect(any(), any());
    }

    @Test
    void withTheApprovalTheFullReportIsWritten() {
        when(requests.canDownloadFull(project, userId)).thenReturn(true);

        assertSame(fullFile, service.fullReport(project.getId(), userId, null));

        verify(summaries).summarize(any(), eq(ReportLanguage.EN));
    }

    @Test
    void aSharedLinkOnlyOpensThePublicReport() {
        when(shares.projectOf("abc")).thenReturn(project.getId());

        assertSame(publicFile, service.sharedReport("abc", "es"));

        verify(data).collect(project.getId(), ReportType.PUBLIC);
        verify(fullWriter, never()).write(any(), any());
    }
}
