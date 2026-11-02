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

/** Writes the email that confirms the deletion of an account. */
@Service
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    private final EmailSender emailSender;
    private final String frontendBaseUrl;

    public AccountEmailService(EmailSender emailSender, @Value("${app.frontend-url}") String frontendUrls) {
        this.emailSender = emailSender;
        this.frontendBaseUrl = EmailText.firstUrl(frontendUrls);
    }

    /** Like the project deletion, a failure cancels the request: without the email it cannot be confirmed. */
    public void sendDeletionConfirmation(User user, String token, Duration linkTtl, Duration purgeAfter) {
        String link = frontendBaseUrl + "/confirm-account-deletion?token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        String html = """
                <p>Hi %s,</p>
                <p>You asked to delete your ObraIA account.</p>
                <p><a href="%s">Confirm the deletion of my account</a></p>
                <p>The link expires in %d minutes. After you confirm, you cannot sign in anymore,
                the projects you created are deleted too and your data is removed for good after %d days.</p>
                <p>If you did not ask for this, change your password: someone may be using your session.</p>
                """.formatted(EmailText.escape(user.getFirstName()), link, linkTtl.toMinutes(), purgeAfter.toDays());
        EmailMessage message = new EmailMessage(user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                "Confirm the deletion of your ObraIA account", html);
        try {
            emailSender.send(message);
        } catch (EmailDeliveryException ex) {
            log.error("Could not send the account deletion confirmation to {}", user.getEmail(), ex);
            throw new EmailNotSentException(ex);
        }
    }
}
