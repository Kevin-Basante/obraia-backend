package com.obraia.backend.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.JwtProperties;
import com.obraia.backend.entity.RefreshToken;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.RefreshTokenRepository;
import com.obraia.backend.security.TokenHasher;

/**
 * Issues, rotates and revokes refresh tokens. Rotation means each token
 * works once: if an old token is used again, someone may have stolen it,
 * so every session of that user is closed.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Creates a token for the user and returns the plain value (only the hash is saved). */
    @Transactional
    public String issue(User user) {
        String token = TokenHasher.generateToken();
        Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());
        repository.save(new RefreshToken(user, TokenHasher.hash(token), expiresAt));
        return token;
    }

    /** Revokes the token and issues a new one for the same user. */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String token) {
        Instant now = Instant.now();
        RefreshToken current = repository.findByTokenHash(TokenHasher.hash(token))
                .orElseThrow(InvalidTokenException::refreshToken);

        if (current.isRevoked()) {
            UUID userId = current.getUser().getId();
            log.warn("Revoked refresh token reused for user {}: closing every session", userId);
            repository.revokeAllByUserId(userId, now);
            throw InvalidTokenException.refreshToken();
        }
        if (current.isExpired(now)) {
            throw InvalidTokenException.refreshToken();
        }

        current.revoke(now);
        User user = current.getUser();
        return new Rotation(user, issue(user));
    }

    /** Revokes one token (sign out). Returns the owner, or empty if the token is unknown. */
    @Transactional
    public Optional<UUID> revoke(String token) {
        return repository.findByTokenHash(TokenHasher.hash(token)).map(refreshToken -> {
            refreshToken.revoke(Instant.now());
            return refreshToken.getUser().getId();
        });
    }

    /** Closes every session of the user (for example, after a password change). */
    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }

    /**
     * @param user     owner of the token
     * @param newToken plain value of the new refresh token
     */
    public record Rotation(User user, String newToken) {
    }
}
