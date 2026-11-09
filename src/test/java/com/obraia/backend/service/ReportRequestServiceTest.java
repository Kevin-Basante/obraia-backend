package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ReportProperties;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.report.FullReportAccessResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.report.ReportRequest;
import com.obraia.backend.exception.BadRequestException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.ReportRequestRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.TokenHasher;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class ReportRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:00:00Z");
    private static final ReportProperties PROPERTIES = new ReportProperties(Duration.ofHours(24),
            Duration.ofHours(24), Duration.ofDays(7), Duration.ofSeconds(45));

    private final User owner = TestUsers.verifiedUser("hash");
    private final User member = TestUsers.withId(new User("luis@example.com", "hash", "Luis", "Gomez", null));
    private final Project project = ProjectFixtures.project(owner.getId(), "Casa Prueba");

    private ReportRequestRepository requests;
    private ProjectRepository projects;
    private UserRepository users;
    private ProjectEmailService emails;
    private ReportRequestService service;

    @BeforeEach
    void setUp() {
        requests = mock(ReportRequestRepository.class);
        projects = mock(ProjectRepository.class);
        users = mock(UserRepository.class);
        emails = mock(ProjectEmailService.class);
        ProjectAccessService access = mock(ProjectAccessService.class);
        when(access.requireProject(project.getId(), owner.getId())).thenReturn(project);
        when(access.requireProject(project.getId(), member.getId())).thenReturn(project);
        when(users.findByIdAndDeletedAtIsNull(owner.getId())).thenReturn(Optional.of(owner));
        when(users.findByIdAndDeletedAtIsNull(member.getId())).thenReturn(Optional.of(member));
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        service = new ReportRequestService(requests, projects, users, access, emails, PROPERTIES,
                Clock.fixed(NOW, ClockConfig.ZONE));
    }

    @Test
    void theCreatorAlwaysCanDownload() {
        FullReportAccessResponse access = service.access(project.getId(), owner.getId());

        assertEquals(FullReportAccessResponse.OWNER, access.status());
        assertTrue(service.canDownloadFull(project, owner.getId()));
    }

    @Test
    void aMemberWithoutRequestCannot() {
        FullReportAccessResponse access = service.access(project.getId(), member.getId());

        assertEquals(FullReportAccessResponse.NONE, access.status());
        assertFalse(service.canDownloadFull(project, member.getId()));
    }

    @Test
    void aRequestIsPendingUntilTheLinkExpires() {
        givenLatest(request(NOW.plusSeconds(60)));
        assertEquals(FullReportAccessResponse.PENDING, service.access(project.getId(), member.getId()).status());

        givenLatest(request(NOW.minusSeconds(1)));
        assertEquals(FullReportAccessResponse.EXPIRED, service.access(project.getId(), member.getId()).status());
    }

    @Test
    void anApprovedRequestAllowsTheDownloadDuringTheWindow() {
        ReportRequest approved = request(NOW.plusSeconds(60));
        approved.approve(NOW.minusSeconds(3600), Duration.ofHours(24));
        givenLatest(approved);

        FullReportAccessResponse access = service.access(project.getId(), member.getId());

        assertEquals(FullReportAccessResponse.APPROVED, access.status());
        assertEquals(NOW.plus(Duration.ofHours(23)), access.downloadUntil());
        assertTrue(service.canDownloadFull(project, member.getId()));
    }

    @Test
    void afterTheWindowTheApprovalExpires() {
        ReportRequest approved = request(NOW.plusSeconds(60));
        approved.approve(NOW.minus(Duration.ofHours(25)), Duration.ofHours(24));
        givenLatest(approved);

        FullReportAccessResponse access = service.access(project.getId(), member.getId());

        assertEquals(FullReportAccessResponse.EXPIRED, access.status());
        assertNull(access.downloadUntil());
    }

    @Test
    void requestStoresOnlyTheHashAndEmailsTheCreator() {
        MessageResponse response = service.request(project.getId(), member.getId());

        assertEquals("report_request_sent", response.code());
        assertEquals(Map.of("owner", "Ana", "project", "Casa Prueba"), response.params());
        verify(requests).deletePending(project.getId(), member.getId());
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emails).sendReportApproval(eq(owner), eq(member), eq("Casa Prueba"), token.capture(),
                eq(Duration.ofHours(24)), eq(Duration.ofHours(24)));
        ArgumentCaptor<ReportRequest> saved = ArgumentCaptor.forClass(ReportRequest.class);
        verify(requests).save(saved.capture());
        assertEquals(TokenHasher.hash(token.getValue()), saved.getValue().getTokenHash());
        assertNotEquals(token.getValue(), saved.getValue().getTokenHash());
        assertEquals(NOW.plus(Duration.ofHours(24)), saved.getValue().getLinkExpiresAt());
    }

    @Test
    void theCreatorDoesNotNeedToAsk() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.request(project.getId(), owner.getId()));

        assertEquals("REPORT_ALREADY_ALLOWED", ex.getCode());
        verify(emails, never()).sendReportApproval(any(), any(), any(), any(), any(), any());
    }

    @Test
    void withoutTheCreatorAccountNobodyCanApprove() {
        when(users.findByIdAndDeletedAtIsNull(owner.getId())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.request(project.getId(), member.getId()));

        assertEquals("PROJECT_OWNER_NOT_FOUND", ex.getCode());
        verify(requests, never()).save(any());
    }

    @Test
    void approvingOpensTheDownloadWindow() {
        ReportRequest pending = request(NOW.plusSeconds(60));
        when(requests.findByTokenHash(TokenHasher.hash("token-123"))).thenReturn(Optional.of(pending));

        MessageResponse response = service.approve("token-123");

        assertEquals("report_request_approved", response.code());
        assertEquals(Map.of("requester", "Luis", "project", "Casa Prueba", "hours", "24"), response.params());
        assertEquals(ReportRequest.APPROVED, pending.getStatus());
        assertEquals(NOW.plus(Duration.ofHours(24)), pending.getDownloadUntil());
    }

    @Test
    void anUsedOrExpiredLinkIsRejected() {
        ReportRequest used = request(NOW.plusSeconds(60));
        used.approve(NOW, Duration.ofHours(24));
        when(requests.findByTokenHash(TokenHasher.hash("used"))).thenReturn(Optional.of(used));
        when(requests.findByTokenHash(TokenHasher.hash("old"))).thenReturn(Optional.of(request(NOW)));
        when(requests.findByTokenHash(TokenHasher.hash("nope"))).thenReturn(Optional.empty());

        for (String token : new String[] { "used", "old", "nope" }) {
            InvalidTokenException ex = assertThrows(InvalidTokenException.class, () -> service.approve(token));
            assertEquals("INVALID_REPORT_TOKEN", ex.getCode());
        }
    }

    @Test
    void theLinkOfADeletedProjectIsRejected() {
        when(requests.findByTokenHash(anyString())).thenReturn(Optional.of(request(NOW.plusSeconds(60))));
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.approve("token-123"));
    }

    private ReportRequest request(Instant linkExpiresAt) {
        return new ReportRequest(project.getId(), member.getId(), TokenHasher.hash("token-123"), linkExpiresAt,
                NOW.minusSeconds(600));
    }

    private void givenLatest(ReportRequest request) {
        when(requests.findFirstByProjectIdAndRequestedByOrderByCreatedAtDesc(project.getId(), member.getId()))
                .thenReturn(Optional.of(request));
    }
}
