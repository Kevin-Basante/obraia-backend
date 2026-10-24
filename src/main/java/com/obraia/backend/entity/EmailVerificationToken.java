package com.obraia.backend.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Link sent after registration to confirm that the email belongs to the user. */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends OneTimeToken {

    protected EmailVerificationToken() {
        // Required by JPA
    }

    public EmailVerificationToken(User user, String tokenHash, Instant expiresAt) {
        super(user, tokenHash, expiresAt);
    }
}
