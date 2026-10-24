package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.obraia.backend.config.AuthProperties;
import com.obraia.backend.dto.auth.AuthResponse;
import com.obraia.backend.dto.auth.LoginRequest;
import com.obraia.backend.dto.auth.RegisterRequest;
import com.obraia.backend.dto.auth.ResetPasswordRequest;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.EmailVerificationToken;
import com.obraia.backend.entity.PasswordResetToken;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.EmailAlreadyRegisteredException;
import com.obraia.backend.exception.EmailNotVerifiedException;
import com.obraia.backend.exception.InvalidCredentialsException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.EmailVerificationTokenRepository;
import com.obraia.backend.repository.PasswordResetTokenRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.security.JwtService;
import com.obraia.backend.security.TokenHasher;
import com.obraia.backend.support.TestUsers;

/** Tests the authentication rules with fake repositories, so no database is needed. */
class AuthServiceTest {

    // Low cost so the tests run fast; the application uses the default cost.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final ClientInfo client = new ClientInfo("127.0.0.1", "JUnit");

    private UserRepository users;
    private RoleRepository roles;
    private EmailVerificationTokenRepository verificationTokens;
    private PasswordResetTokenRepository resetTokens;
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
        resetTokens = mock(PasswordResetTokenRepository.class);
        refreshTokenService = mock(RefreshTokenService.class);
        jwtService = mock(JwtService.class);
        emails = mock(AuthEmailService.class);
        audit = mock(AuditService.class);
        AuthProperties properties = new AuthProperties(Duration.ofHours(24), Duration.ofMinutes(30));
        service = new AuthService(users, roles, verificationTokens, resetTokens, refreshTokenService, jwtService,
                passwordEncoder, emails, audit, properties);
    }

    @Test
    void registerRejectsAnEmailThatAlreadyExists() {
        when(users.existsByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Ana", "Perez", " Ana@Example.com ", "secreto123", null);

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(request, client));
        verify(users, never()).save(any());
    }

    @Test
    void loginWithWrongPasswordFailsAndIsRecorded() {
        User user = TestUsers.verifiedUser(passwordEncoder.encode("secreto123"));
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("ana@example.com", "wrong1234");

        assertThrows(InvalidCredentialsException.class, () -> service.login(request, client));
        verify(audit).recordFailure(AuditEvent.LOGIN_FAILED, user.getId(), "ana@example.com", client);
    }

    @Test
    void loginWithUnknownEmailGivesTheSameError() {
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("nobody@example.com", "secreto123");

        assertThrows(InvalidCredentialsException.class, () -> service.login(request, client));
        verify(audit).recordFailure(eq(AuditEvent.LOGIN_FAILED), isNull(), eq("nobody@example.com"), eq(client));
    }

    @Test
    void loginRequiresAVerifiedEmail() {
        User user = TestUsers.unverifiedUser(passwordEncoder.encode("secreto123"));
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest("ana@example.com", "secreto123");

        assertThrows(EmailNotVerifiedException.class, () -> service.login(request, client));
    }

    @Test
    void successfulLoginReturnsBothTokens() {
        User user = TestUsers.verifiedUser(passwordEncoder.encode("secreto123"));
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user)).thenReturn(new JwtService.AccessToken("jwt", 900));
        when(refreshTokenService.issue(user)).thenReturn("refresh");

        AuthResponse response = service.login(new LoginRequest("ANA@example.com", "secreto123"), client);

        assertEquals("jwt", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void verifyEmailMarksTheUserAndTheToken() {
        User user = TestUsers.unverifiedUser("hash");
        EmailVerificationToken token = new EmailVerificationToken(user, TokenHasher.hash("abc"),
                Instant.now().plusSeconds(60));
        when(verificationTokens.findByTokenHash(TokenHasher.hash("abc"))).thenReturn(Optional.of(token));

        service.verifyEmail("abc", client);

        assertTrue(user.isEmailVerified());
        assertNotNull(token.getUsedAt());
    }

    @Test
    void expiredVerificationLinkIsRejected() {
        EmailVerificationToken token = new EmailVerificationToken(TestUsers.unverifiedUser("hash"),
                TokenHasher.hash("abc"), Instant.now().minusSeconds(1));
        when(verificationTokens.findByTokenHash(TokenHasher.hash("abc"))).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> service.verifyEmail("abc", client));
    }

    @Test
    void forgotPasswordGivesTheSameAnswerForUnknownEmails() {
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

        assertEquals(AuthService.RESET_SENT, service.forgotPassword("nobody@example.com", client).message());
        verify(emails, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPasswordChangesTheHashAndClosesEverySession() {
        User user = TestUsers.verifiedUser(passwordEncoder.encode("secreto123"));
        PasswordResetToken token = new PasswordResetToken(user, TokenHasher.hash("abc"), Instant.now().plusSeconds(60));
        when(resetTokens.findByTokenHash(TokenHasher.hash("abc"))).thenReturn(Optional.of(token));

        service.resetPassword(new ResetPasswordRequest("abc", "nuevaClave9"), client);

        assertTrue(passwordEncoder.matches("nuevaClave9", user.getPasswordHash()));
        verify(refreshTokenService).revokeAll(user.getId());
    }

    @Test
    void usedResetLinkIsRejected() {
        PasswordResetToken token = new PasswordResetToken(TestUsers.verifiedUser("hash"), TokenHasher.hash("abc"),
                Instant.now().plusSeconds(60));
        token.markUsed(Instant.now());
        when(resetTokens.findByTokenHash(TokenHasher.hash("abc"))).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest("abc", "nuevaClave9");

        assertThrows(InvalidTokenException.class, () -> service.resetPassword(request, client));
    }
}
