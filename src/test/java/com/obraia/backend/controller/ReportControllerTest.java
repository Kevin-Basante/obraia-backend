package com.obraia.backend.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.report.FullReportAccessResponse;
import com.obraia.backend.dto.report.ShareLinkResponse;
import com.obraia.backend.exception.ForbiddenException;
import com.obraia.backend.exception.GlobalExceptionHandler;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.report.ReportFile;
import com.obraia.backend.service.ReportRequestService;
import com.obraia.backend.service.ReportService;
import com.obraia.backend.service.ReportShareService;

/** HTTP side of the report routes, without the database. */
class ReportControllerTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final ReportFile file = new ReportFile("casa-public-report.pdf", "%PDF-1.4".getBytes());

    private ReportService reports;
    private ReportRequestService requests;
    private ReportShareService shares;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        reports = mock(ReportService.class);
        requests = mock(ReportRequestService.class);
        shares = mock(ReportShareService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportController(reports, requests, shares))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void thePublicReportIsADownloadablePdf() throws Exception {
        when(reports.publicReport(projectId, userId, "es")).thenReturn(file);

        // The frontend sends Accept: application/json on every request, files too.
        mvc.perform(get("/api/projects/{id}/reports/public", projectId).param("lang", "es")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"casa-public-report.pdf\""))
                .andExpect(content().bytes(file.content()));
    }

    @Test
    void theFullReportWithoutApprovalAnswers403() throws Exception {
        when(reports.fullReport(projectId, userId, null))
                .thenThrow(new ForbiddenException("REPORT_NOT_APPROVED", "Not approved"));

        mvc.perform(get("/api/projects/{id}/reports/full", projectId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REPORT_NOT_APPROVED"));
    }

    @Test
    void accessSaysWhereTheRequestStands() throws Exception {
        when(requests.access(projectId, userId)).thenReturn(new FullReportAccessResponse(
                FullReportAccessResponse.PENDING, Instant.parse("2026-11-09T15:00:00Z"), null));

        mvc.perform(get("/api/projects/{id}/reports/full/access", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.requestedAt").value("2026-11-09T15:00:00Z"));
    }

    @Test
    void askingForTheFullReportAnswers202() throws Exception {
        when(requests.request(projectId, userId)).thenReturn(MessageResponse.of("report_request_sent", "Sent",
                Map.of("owner", "Ana", "project", "Casa")));

        mvc.perform(post("/api/projects/{id}/reports/full/requests", projectId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("report_request_sent"))
                .andExpect(jsonPath("$.params.owner").value("Ana"));
    }

    @Test
    void sharingAnswersTheLink() throws Exception {
        when(shares.create(projectId, userId)).thenReturn(new ShareLinkResponse(
                "https://obraia-frontend.vercel.app/shared-report?token=abc", Instant.parse("2026-11-16T15:00:00Z")));

        mvc.perform(post("/api/projects/{id}/reports/share", projectId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://obraia-frontend.vercel.app/shared-report?token=abc"));
    }

    @Test
    void theCreatorApprovesWithTheToken() throws Exception {
        when(requests.approve("abc")).thenReturn(MessageResponse.of("report_request_approved", "Approved",
                Map.of("requester", "Luis")));

        mvc.perform(post("/api/reports/approve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("report_request_approved"));
    }

    @Test
    void approvingWithoutTokenIsRejected() throws Exception {
        mvc.perform(post("/api/reports/approve").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aSharedLinkDownloadsThePublicReport() throws Exception {
        when(reports.sharedReport("abc", "en")).thenReturn(file);

        mvc.perform(post("/api/reports/shared").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"abc\", \"lang\": \"en\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void anExpiredSharedLinkAnswers400() throws Exception {
        when(reports.sharedReport("old", null)).thenThrow(InvalidTokenException.shareLink());

        mvc.perform(post("/api/reports/shared").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"old\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_SHARE_TOKEN"));
    }
}
