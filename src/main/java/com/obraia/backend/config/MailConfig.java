package com.obraia.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.obraia.backend.email.BrevoEmailSender;
import com.obraia.backend.email.EmailSender;
import com.obraia.backend.email.LoggingEmailSender;

/**
 * Chooses how emails are sent. Render's free plan blocks SMTP, so in
 * production the emails go through the Brevo HTTP API. Locally (or before
 * Brevo is configured) they are only written to the log.
 */
@Configuration
public class MailConfig {

    @Bean
    public EmailSender emailSender(MailProperties properties) {
        if (!properties.usesBrevo()) {
            return new LoggingEmailSender();
        }
        if (properties.brevoApiKey() == null || properties.brevoApiKey().isBlank()) {
            throw new IllegalStateException("MAIL_PROVIDER is brevo but BREVO_API_KEY is empty");
        }
        return new BrevoEmailSender(properties);
    }
}
