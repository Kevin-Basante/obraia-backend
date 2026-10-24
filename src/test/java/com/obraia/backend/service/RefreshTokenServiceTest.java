package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.config.JwtProperties;
import com.obraia.backend.entity.RefreshToken;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.RefreshTokenRepository;
import com.obraia.backend.security.TokenHasher;
import com.obraia.backend.support.TestUsers;

class RefreshTokenServiceTest {

    private RefreshTokenRepository repository;
    private RefreshTokenService service;
    private final User user = TestUsers.verifiedUser("hash");

    @BeforeEach
    void setUp() {
        repository = mock(RefreshTokenRepository.class);
        JwtProperties properties = new JwtProperties("secret", "obraia", Duration.ofMinutes(15), Duration.ofDays(7));
        service = new RefreshTokenService(repository, properties);
    }

    @Test
    void issueSavesOnlyTheHash() {
        String token = service.issue(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertEquals(TokenHasher.hash(token), captor.getValue().getTokenHash());
        assertTrue(captor.getValue().getExpiresAt().isAfter(Instant.now().plus(Duration.ofDays(6))));
    }

    @Test
    void rotateRevokesTheOldTokenAndIssuesANewOne() {
        RefreshToken current = new RefreshToken(user, TokenHasher.hash("old"), Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(TokenHasher.hash("old"))).thenReturn(Optional.of(current));

        RefreshTokenService.Rotation rotation = service.rotate("old");

        assertTrue(current.isRevoked());
        assertEquals(user, rotation.user());
        assertNotNull(rotation.newToken());
    }

    @Test
    void reusingARevokedTokenClosesEverySession() {
        RefreshToken revoked = new RefreshToken(user, TokenHasher.hash("old"), Instant.now().plusSeconds(60));
        revoked.revoke(Instant.now());
        when(repository.findByTokenHash(TokenHasher.hash("old"))).thenReturn(Optional.of(revoked));

        assertThrows(InvalidTokenException.class, () -> service.rotate("old"));
        verify(repository).revokeAllByUserId(eq(user.getId()), any(Instant.class));
    }

    @Test
    void expiredTokenIsRejected() {
        RefreshToken expired = new RefreshToken(user, TokenHasher.hash("old"), Instant.now().minusSeconds(1));
        when(repository.findByTokenHash(TokenHasher.hash("old"))).thenReturn(Optional.of(expired));

        assertThrows(InvalidTokenException.class, () -> service.rotate("old"));
        verify(repository, never()).save(any());
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> service.rotate("unknown"));
    }

    @Test
    void revokeReturnsTheOwner() {
        RefreshToken token = new RefreshToken(user, TokenHasher.hash("abc"), Instant.now().plusSeconds(60));
        when(repository.findByTokenHash(TokenHasher.hash("abc"))).thenReturn(Optional.of(token));

        assertEquals(Optional.of(user.getId()), service.revoke("abc"));
        assertTrue(token.isRevoked());
    }
}
