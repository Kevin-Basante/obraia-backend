package com.obraia.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Email settings (prefix "app.mail").
 *
 * @param provider    "log" writes the emails to the server log; "brevo" sends them
 * @param fromEmail   sender address (must be verified in Brevo)
 * @param fromName    sender name shown in the inbox
 * @param brevoApiKey Brevo API key, only needed when provider is "brevo"
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String provider, String fromEmail, String fromName, String brevoApiKey) {

    public boolean usesBrevo() {
        return "brevo".equalsIgnoreCase(provider);
    }
}
