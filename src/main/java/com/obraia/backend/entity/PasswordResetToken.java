package com.obraia.backend.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Link sent when the user forgets the password. */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends OneTimeToken {

    protected PasswordResetToken() {
        // Required by JPA
    }

    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        super(user, tokenHash, expiresAt);
    }
}
