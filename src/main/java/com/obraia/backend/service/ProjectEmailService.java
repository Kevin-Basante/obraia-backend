package com.obraia.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.obraia.backend.email.EmailDeliveryException;
import com.obraia.backend.email.EmailMessage;
import com.obraia.backend.email.EmailSender;
import com.obraia.backend.email.EmailText;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.EmailNotSentException;

/** Writes the emails about projects. */
@Service
public class ProjectEmailService {

    private static final Logger log = LoggerFactory.getLogger(ProjectEmailService.class);

    private final EmailSender emailSender;
    private final String frontendBaseUrl;

    public ProjectEmailService(EmailSender emailSender, @Value("${app.frontend-url}") String frontendUrls) {
        this.emailSender = emailSender;
        this.frontendBaseUrl = EmailText.firstUrl(frontendUrls);
    }

    /**
     * Link that confirms the deletion. Unlike the account emails, a failure
     * cancels the request: without the email the user cannot confirm it.
     */
    public void sendDeletionConfirmation(User user, String projectName, String token, Duration linkTtl,
            Duration purgeAfter) {
        String link = frontendBaseUrl + "/confirm-deletion?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String html = """
                <p>Hi %s,</p>
                <p>You asked to delete the project <strong>%s</strong> from ObraIA.</p>
                <p><a href="%s">Confirm the deletion</a></p>
                <p>The link expires in %d minutes. After you confirm, the project disappears from your list
                and its data is removed for good after %d days.</p>
                <p>If you did not ask for this, ignore this email: nothing will be deleted.</p>
                """.formatted(EmailText.escape(user.getFirstName()), EmailText.escape(projectName), link,
                linkTtl.toMinutes(), purgeAfter.toDays());
        EmailMessage message = new EmailMessage(user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                "Confirm the deletion of " + projectName, html);
        try {
            emailSender.send(message);
        } catch (EmailDeliveryException ex) {
            log.error("Could not send the deletion confirmation to {}", user.getEmail(), ex);
            throw new EmailNotSentException(ex);
        }
    }
}
