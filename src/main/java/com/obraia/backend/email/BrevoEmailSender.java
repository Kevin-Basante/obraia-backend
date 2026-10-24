package com.obraia.backend.email;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.obraia.backend.config.MailProperties;

/** Sends emails with the Brevo transactional email API (free plan: 300 emails per day). */
public class BrevoEmailSender implements EmailSender {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3";

    private final RestClient restClient;
    private final MailProperties properties;

    public BrevoEmailSender(MailProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader("api-key", properties.brevoApiKey())
                .build();
    }

    @Override
    public void send(EmailMessage message) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", properties.fromName(), "email", properties.fromEmail()),
                "to", List.of(Map.of("email", message.to(), "name", message.toName())),
                "subject", message.subject(),
                "htmlContent", message.htmlContent());
        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new EmailDeliveryException("Brevo did not accept the email", ex);
        }
    }
}
