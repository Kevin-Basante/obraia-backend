package com.obraia.backend.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.repository.EmailVerificationTokenRepository;
import com.obraia.backend.repository.PasswordResetTokenRepository;
import com.obraia.backend.repository.RefreshTokenRepository;

/** Deletes expired tokens every night so the tables do not grow forever. */
@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenRepository refreshTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;

    public TokenCleanupService(RefreshTokenRepository refreshTokens,
            EmailVerificationTokenRepository verificationTokens,
            PasswordResetTokenRepository resetTokens) {
        this.refreshTokens = refreshTokens;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
    }

    /** Every day at 3:30 a.m. Colombia time. */
    @Scheduled(cron = "0 30 3 * * *", zone = "America/Bogota")
    @Transactional
    public void deleteExpiredTokens() {
        Instant now = Instant.now();
        int refresh = refreshTokens.deleteExpired(now);
        int verification = verificationTokens.deleteExpired(now);
        int reset = resetTokens.deleteExpired(now);
        log.info("Expired tokens deleted: {} refresh, {} verification, {} password reset",
                refresh, verification, reset);
    }
}
