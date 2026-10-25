package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.config.ProjectProperties;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.DeletionRequest;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ForbiddenException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectContentRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.TokenHasher;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class ProjectDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");
    private static final ProjectProperties PROPERTIES = new ProjectProperties(Duration.ofMinutes(30),
            Duration.ofDays(30));

    private final User creator = TestUsers.verifiedUser("hash");
    private final Project project = ProjectFixtures.project(creator.getId(), "Casa Prueba");

    private ProjectRepository projects;
    private DeletionRequestRepository requests;
    private ProjectEmailService emails;
    private ProjectAccessService access;
    private ProjectDeletionService service;

    @BeforeEach
    void setUp() {
        projects = mock(ProjectRepository.class);
        requests = mock(DeletionRequestRepository.class);
        emails = mock(ProjectEmailService.class);
        access = mock(ProjectAccessService.class);
        ProjectContentRepository content = mock(ProjectContentRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(content.countContent(project.getId())).thenReturn(Map.of("members", 2));
        when(users.findByIdAndDeletedAtIsNull(creator.getId())).thenReturn(Optional.of(creator));
        when(access.requireProject(project.getId(), creator.getId())).thenReturn(project);
        service = new ProjectDeletionService(projects, content, requests, users, access, emails, PROPERTIES,
                Clock.fixed(NOW, ClockConfig.ZONE));
    }

    @Test
    void impactShowsTheContentAndThePurgeDate() {
        var impact = service.impact(project.getId(), creator.getId());

        assertEquals(2, impact.content().get("members"));
        assertEquals(LocalDate.of(2026, 12, 2), impact.purgeDate());
        assertEquals(30, impact.linkValidMinutes());
    }

    @Test
    void onlyTheCreatorCanAskForTheDeletion() {
        UUID partner = UUID.randomUUID();
        when(access.requireProject(project.getId(), partner)).thenReturn(project);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> service.requestDeletion(project.getId(), partner));

        assertEquals("NOT_PROJECT_CREATOR", ex.getCode());
        verify(emails, never()).sendDeletionConfirmation(any(), any(), any(), any(), any());
    }

    @Test
    void requestStoresOnlyTheHashAndEmailsTheToken() {
        service.requestDeletion(project.getId(), creator.getId());

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emails).sendDeletionConfirmation(eq(creator), eq("Casa Prueba"), token.capture(), any(), any());
        ArgumentCaptor<DeletionRequest> saved = ArgumentCaptor.forClass(DeletionRequest.class);
        verify(requests).save(saved.capture());
        verify(requests).deletePending(DeletionRequest.TARGET_PROJECT, project.getId());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), saved.getValue().getExpiresAt());
        assertEquals(project.getId(), saved.getValue().getTargetId());
        assertNotEquals(token.getValue(), TokenHasher.hash(token.getValue()));
    }

    @Test
    void confirmingHidesTheProject() {
        givenRequest(NOW.plusSeconds(60));
        when(projects.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));

        String message = service.confirmDeletion("token-123").message();

        assertTrue(project.isDeleted());
        assertTrue(message.contains("2026-12-02"));
    }

    @Test
    void expiredLinkIsRejected() {
        givenRequest(NOW.minusSeconds(1));

        InvalidTokenException ex = assertThrows(InvalidTokenException.class,
                () -> service.confirmDeletion("token-123"));

        assertEquals("INVALID_DELETION_TOKEN", ex.getCode());
        assertFalse(project.isDeleted());
    }

    @Test
    void unknownLinkIsRejected() {
        when(requests.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.confirmDeletion("nope"));
    }

    private void givenRequest(Instant expiresAt) {
        DeletionRequest request = new DeletionRequest(creator.getId(), DeletionRequest.TARGET_PROJECT,
                project.getId(), TokenHasher.hash("token-123"), expiresAt);
        when(requests.findByTokenHash(TokenHasher.hash("token-123"))).thenReturn(Optional.of(request));
    }
}
