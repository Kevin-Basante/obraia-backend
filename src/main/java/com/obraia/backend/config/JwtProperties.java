package com.obraia.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings read from application.properties (prefix "app.jwt").
 *
 * @param secret          HMAC key used to sign the tokens (at least 32 characters)
 * @param issuer          value of the "iss" claim, checked when a token is read
 * @param accessTokenTtl  how long an access token is valid
 * @param refreshTokenTtl how long a refresh token is valid
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
