package com.obraia.backend.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lifetimes of the one-time links sent by email (prefix "app.auth").
 *
 * @param verificationTokenTtl  validity of the email verification link
 * @param passwordResetTokenTtl validity of the password reset link
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(Duration verificationTokenTtl, Duration passwordResetTokenTtl) {
}
