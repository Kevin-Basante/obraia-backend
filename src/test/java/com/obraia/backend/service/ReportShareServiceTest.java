package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ReportProperties;
import com.obraia.backend.dto.report.ShareLinkResponse;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.entity.report.ReportShare;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.ReportShareRepository;
import com.obraia.backend.security.TokenHasher;
import com.obraia.backend.support.ProjectFixtures;

class ReportShareServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:00:00Z");
    private static final ReportProperties PROPERTIES = new ReportProperties(Duration.ofHours(24),
            Duration.ofHours(24), Duration.ofDays(7), Duration.ofSeconds(45));

    private final UUID userId = UUID.randomUUID();
    private final Project project = ProjectFixtures.project(userId, "Casa Prueba");

    private ReportShareRepository shares;
    private ProjectRepository projects;
    private ProjectAccessService access;
    private ReportShareService service;

    @BeforeEach
    void setUp() {
        shares = mock(ReportShareRepository.class);
        projects = mock(ProjectRepository.class);
        access = mock(ProjectAccessService.class);
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        service = new ReportShareService(shares, projects, access, PROPERTIES, Clock.fixed(NOW, ClockConfig.ZONE),
                "https://obraia-frontend.vercel.app/,http://localhost:5173");
    }

    @Test
    void aMemberGetsALinkAndOnlyItsHashIsStored() {
        ShareLinkResponse link = service.create(project.getId(), userId);

        verify(access).requireMembership(project.getId(), userId);
        assertTrue(link.url().startsWith("https://obraia-frontend.vercel.app/shared-report?token="));
        assertEquals(NOW.plus(Duration.ofDays(7)), link.expiresAt());
        String token = link.url().substring(link.url().indexOf("token=") + 6);
        ArgumentCaptor<ReportShare> saved = ArgumentCaptor.forClass(ReportShare.class);
        verify(shares).save(saved.capture());
        assertEquals(TokenHasher.hash(token), saved.getValue().getTokenHash());
        assertFalse(saved.getValue().getTokenHash().equals(token));
    }

    @Test
    void aWorkingLinkGivesItsProject() {
        when(shares.findByTokenHash(TokenHasher.hash("abc"))).thenReturn(Optional.of(share(NOW.plusSeconds(60))));

        assertEquals(project.getId(), service.projectOf("abc"));
    }

    @Test
    void anExpiredOrUnknownLinkIsRejected() {
        when(shares.findByTokenHash(TokenHasher.hash("old"))).thenReturn(Optional.of(share(NOW)));
        when(shares.findByTokenHash(TokenHasher.hash("nope"))).thenReturn(Optional.empty());

        assertEquals("INVALID_SHARE_TOKEN",
                assertThrows(InvalidTokenException.class, () -> service.projectOf("old")).getCode());
        assertThrows(InvalidTokenException.class, () -> service.projectOf("nope"));
    }

    @Test
    void theLinkOfADeletedProjectStopsWorking() {
        when(shares.findByTokenHash(anyString())).thenReturn(Optional.of(share(NOW.plusSeconds(60))));
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.projectOf("abc"));
    }

    private ReportShare share(Instant expiresAt) {
        return new ReportShare(project.getId(), userId, TokenHasher.hash("abc"), expiresAt, NOW.minusSeconds(60));
    }
}
