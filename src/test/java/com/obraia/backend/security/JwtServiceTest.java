package com.obraia.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.obraia.backend.config.JwtConfig;
import com.obraia.backend.config.JwtProperties;
import com.obraia.backend.support.TestUsers;

/** Creates real tokens and reads them back with the same configuration as the application. */
class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";

    private JwtProperties properties;
    private JwtService jwtService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(SECRET, "obraia-test", Duration.ofMinutes(15), Duration.ofDays(7));
        JwtConfig config = new JwtConfig();
        SecretKey key = config.jwtSecretKey(properties);
        jwtService = new JwtService(config.jwtEncoder(key), properties);
        decoder = config.jwtDecoder(key, properties);
    }

    @Test
    void tokenContainsUserIdEmailAndRoles() {
        var user = TestUsers.verifiedUser("hash");

        JwtService.AccessToken accessToken = jwtService.createAccessToken(user);
        Jwt jwt = decoder.decode(accessToken.token());

        assertEquals(user.getId().toString(), jwt.getSubject());
        assertEquals("ana@example.com", jwt.getClaimAsString("email"));
        assertEquals(List.of(), jwt.getClaimAsStringList("roles"));
        assertEquals(900, accessToken.expiresInSeconds());
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtProperties other = new JwtProperties(SECRET, "someone-else", Duration.ofMinutes(15), Duration.ofDays(7));
        JwtConfig config = new JwtConfig();
        JwtService otherService = new JwtService(config.jwtEncoder(config.jwtSecretKey(other)), other);

        String token = otherService.createAccessToken(TestUsers.verifiedUser("hash")).token();

        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    @Test
    void shortSecretIsRejectedOnStartup() {
        JwtProperties weak = new JwtProperties("short", "obraia-test", Duration.ofMinutes(15), Duration.ofDays(7));

        assertThrows(IllegalStateException.class, () -> new JwtConfig().jwtSecretKey(weak));
    }
}
