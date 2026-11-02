package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.obraia.backend.dto.user.ChangePasswordRequest;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.support.TestUsers;

class AccountServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final ClientInfo client = new ClientInfo("127.0.0.1", "JUnit");
    private final User user = TestUsers.verifiedUser(passwordEncoder.encode("Password123"));

    private RefreshTokenService refreshTokens;
    private AuditService audit;
    private AuthEmailService emails;
    private AccountService service;

    @BeforeEach
    void setUp() {
        UserRepository users = mock(UserRepository.class);
        refreshTokens = mock(RefreshTokenService.class);
        audit = mock(AuditService.class);
        emails = mock(AuthEmailService.class);
        when(users.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        service = new AccountService(users, passwordEncoder, refreshTokens, audit, emails);
    }

    @Test
    void changePasswordSavesTheNewOneAndClosesTheSessions() {
        service.changePassword(user.getId(), new ChangePasswordRequest("Password123", "NewPassword456"), client);

        assertTrue(passwordEncoder.matches("NewPassword456", user.getPasswordHash()));
        verify(refreshTokens).revokeAll(user.getId());
        verify(audit).record(AuditEvent.PASSWORD_CHANGED, user.getId(), "ana@example.com", client);
        verify(emails).sendPasswordChangedEmail(user);
    }

    @Test
    void aWrongCurrentPasswordChangesNothing() {
        FieldValidationException ex = assertThrows(FieldValidationException.class, () -> service
                .changePassword(user.getId(), new ChangePasswordRequest("Wrong123", "NewPassword456"), client));

        assertEquals(Map.of("currentPassword", "is not correct"), ex.getFields());
        assertTrue(passwordEncoder.matches("Password123", user.getPasswordHash()));
        verify(refreshTokens, never()).revokeAll(any());
    }

    @Test
    void theNewPasswordMustBeDifferent() {
        FieldValidationException ex = assertThrows(FieldValidationException.class, () -> service
                .changePassword(user.getId(), new ChangePasswordRequest("Password123", "Password123"), client));

        assertEquals(Map.of("newPassword", "must be different from the current password"), ex.getFields());
    }
}
