package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.obraia.backend.config.AuthProperties;
import com.obraia.backend.dto.auth.AuthResponse;
import com.obraia.backend.dto.auth.LoginRequest;
import com.obraia.backend.dto.auth.RegisterRequest;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.AccountDisabledException;
import com.obraia.backend.repository.EmailVerificationTokenRepository;
import com.obraia.backend.repository.PasswordResetTokenRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.security.JwtService;
import com.obraia.backend.support.TestUsers;

/** Registration, new verification links, token renewal and sign out. */
class AuthServiceSessionsTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final ClientInfo client = new ClientInfo("127.0.0.1", "JUnit");

    private UserRepository users;
    private RoleRepository roles;
    private EmailVerificationTokenRepository verificationTokens;
    private RefreshTokenService refreshTokenService;
    private JwtService jwtService;
    private AuthEmailService emails;
    private AuditService audit;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        roles = mock(RoleRepository.class);
        verificationTokens = mock(EmailVerificationTokenRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        jwtService = mock(JwtService.class);
        emails = mock(AuthEmailService.class);
        audit = mock(AuditService.class);
        service = new AuthService(users, roles, verificationTokens, mock(PasswordResetTokenRepository.class),
                refreshTokenService, jwtService, passwordEncoder, emails, audit,
                new AuthProperties(Duration.ofHours(24), Duration.ofMinutes(30)));
    }

    @Test
    void registerSavesTheUserWithTheUserRoleAndSendsTheLink() {
        when(roles.findByName(Role.USER)).thenReturn(Optional.of(mock(Role.class)));

        service.register(new RegisterRequest(" Ana ", "Rojas", " ANA@Example.com ", "Password123", " "), client);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User user = saved.getValue();
        assertEquals("ana@example.com", user.getEmail());
        assertEquals("Ana", user.getFirstName());
        assertEquals(null, user.getPhone());
        assertEquals(1, user.getRoles().size());
        assertTrue(passwordEncoder.matches("Password123", user.getPasswordHash()));
        verify(emails).sendVerificationEmail(eq(user), anyString());
        verify(audit).record(eq(AuditEvent.REGISTER), any(), eq("ana@example.com"), eq(client));
    }

    @Test
    void resendSendsANewLinkOnlyToUnverifiedAccounts() {
        User pending = TestUsers.unverifiedUser("hash");
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(pending));

        service.resendVerification("ana@example.com");

        verify(verificationTokens).invalidateUnusedByUserId(eq(pending.getId()), any());
        verify(emails).sendVerificationEmail(eq(pending), anyString());
    }

    @Test
    void resendDoesNothingForAVerifiedAccount() {
        User verified = TestUsers.verifiedUser("hash");
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(verified));

        String message = service.resendVerification("ana@example.com").message();

        verify(emails, never()).sendVerificationEmail(any(), any());
        assertEquals(message, service.resendVerification("nobody@example.com").message());
    }

    @Test
    void refreshReturnsNewTokens() {
        User user = TestUsers.verifiedUser("hash");
        when(refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(user, "new"));
        when(jwtService.createAccessToken(user)).thenReturn(new JwtService.AccessToken("jwt", 900));

        AuthResponse response = service.refresh("old", client);

        assertEquals("new", response.refreshToken());
        assertEquals("jwt", response.accessToken());
        verify(audit).record(AuditEvent.TOKEN_REFRESHED, user.getId(), "ana@example.com", client);
    }

    @Test
    void refreshOfADeletedAccountClosesEverySession() {
        User user = TestUsers.verifiedUser("hash");
        user.markDeleted(Instant.now());
        when(refreshTokenService.rotate("old")).thenReturn(new RefreshTokenService.Rotation(user, "new"));

        assertThrows(AccountDisabledException.class, () -> service.refresh("old", client));
        verify(refreshTokenService).revokeAll(user.getId());
    }

    @Test
    void loginOfADisabledAccountIsRejected() {
        User user = TestUsers.verifiedUser(passwordEncoder.encode("Password123"));
        user.markDeleted(Instant.now());
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(user));

        assertThrows(AccountDisabledException.class,
                () -> service.login(new LoginRequest("ana@example.com", "Password123"), client));
    }

    @Test
    void logoutRecordsOnlyKnownTokens() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenService.revoke("known")).thenReturn(Optional.of(userId));
        when(refreshTokenService.revoke("unknown")).thenReturn(Optional.empty());

        service.logout("known", client);
        service.logout("unknown", client);

        verify(audit).record(AuditEvent.LOGOUT, userId, null, client);
        verify(audit, never()).record(eq(AuditEvent.LOGOUT), eq(null), any(), any());
    }
}
