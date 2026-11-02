package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.obraia.backend.config.AccountProperties;
import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.dto.user.DeleteAccountRequest;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.DeletionRequest;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.repository.DeletionRequestRepository;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.ProjectRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.security.TokenHasher;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

class AccountDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");
    private static final AccountProperties PROPERTIES = new AccountProperties(Duration.ofMinutes(30),
            Duration.ofDays(30));

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final ClientInfo client = new ClientInfo("127.0.0.1", "JUnit");
    private final User user = TestUsers.verifiedUser(passwordEncoder.encode("Password123"));

    private UserRepository users;
    private ProjectRepository projects;
    private ProjectMemberRepository members;
    private DeletionRequestRepository requests;
    private RefreshTokenService refreshTokens;
    private AuditService audit;
    private AccountEmailService emails;
    private AccountDeletionService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        projects = mock(ProjectRepository.class);
        members = mock(ProjectMemberRepository.class);
        requests = mock(DeletionRequestRepository.class);
        refreshTokens = mock(RefreshTokenService.class);
        audit = mock(AuditService.class);
        emails = mock(AccountEmailService.class);
        when(users.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        service = new AccountDeletionService(users, projects, members, requests, passwordEncoder, refreshTokens,
                audit, emails, PROPERTIES, Clock.fixed(NOW, ClockConfig.ZONE));
    }

    @Test
    void impactListsTheProjectsThatAreLost() {
        when(projects.findOwnedBy(user.getId())).thenReturn(List.of(
                ProjectFixtures.project(user.getId(), "Casa Norte"),
                ProjectFixtures.project(user.getId(), "Casa Sur")));
        when(projects.findSharedOwnedBy(user.getId()))
                .thenReturn(List.of(ProjectFixtures.project(user.getId(), "Casa Sur")));
        when(projects.countJoinedBy(user.getId())).thenReturn(3L);

        var impact = service.impact(user.getId());

        assertEquals(List.of("Casa Norte", "Casa Sur"), impact.ownProjects());
        assertEquals(List.of("Casa Sur"), impact.sharedProjects());
        assertEquals(3, impact.joinedProjects());
        assertEquals(LocalDate.of(2026, 12, 2), impact.purgeDate());
        assertEquals(30, impact.linkValidMinutes());
    }

    @Test
    void requestStoresOnlyTheHashAndEmailsTheToken() {
        service.requestDeletion(user.getId(), new DeleteAccountRequest("Password123"), client);

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(emails).sendDeletionConfirmation(eq(user), token.capture(), any(), any());
        ArgumentCaptor<DeletionRequest> saved = ArgumentCaptor.forClass(DeletionRequest.class);
        verify(requests).save(saved.capture());
        verify(requests).deletePending(DeletionRequest.TARGET_ACCOUNT, user.getId());
        assertEquals(DeletionRequest.TARGET_ACCOUNT, saved.getValue().getTargetType());
        assertEquals(user.getId(), saved.getValue().getTargetId());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), saved.getValue().getExpiresAt());
        assertNotEquals(token.getValue(), TokenHasher.hash(token.getValue()));
        verify(audit).record(AuditEvent.DELETION_REQUESTED, user.getId(), "ana@example.com", client);
    }

    @Test
    void aWrongPasswordSendsNothing() {
        FieldValidationException ex = assertThrows(FieldValidationException.class,
                () -> service.requestDeletion(user.getId(), new DeleteAccountRequest("Wrong123"), client));

        assertEquals("is not correct", ex.getFields().get("password"));
        verify(emails, never()).sendDeletionConfirmation(any(), any(), any(), any());
        verify(requests, never()).save(any());
    }
}
