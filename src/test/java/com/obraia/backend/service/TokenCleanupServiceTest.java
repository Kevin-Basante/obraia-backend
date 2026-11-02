package com.obraia.backend.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.obraia.backend.config.ClockConfig;
import com.obraia.backend.repository.EmailVerificationTokenRepository;
import com.obraia.backend.repository.PasswordResetTokenRepository;
import com.obraia.backend.repository.RefreshTokenRepository;

class TokenCleanupServiceTest {

    @Test
    void deletesTheExpiredTokensOfTheThreeKinds() {
        Instant now = Instant.parse("2026-11-02T08:30:00Z");
        RefreshTokenRepository refresh = mock(RefreshTokenRepository.class);
        EmailVerificationTokenRepository verification = mock(EmailVerificationTokenRepository.class);
        PasswordResetTokenRepository reset = mock(PasswordResetTokenRepository.class);
        TokenCleanupService service = new TokenCleanupService(refresh, verification, reset,
                Clock.fixed(now, ClockConfig.ZONE));

        service.deleteExpiredTokens();

        verify(refresh).deleteExpired(now);
        verify(verification).deleteExpired(now);
        verify(reset).deleteExpired(now);
    }
}
