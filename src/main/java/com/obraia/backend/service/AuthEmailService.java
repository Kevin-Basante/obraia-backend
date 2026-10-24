package com.obraia.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.obraia.backend.email.EmailDeliveryException;
import com.obraia.backend.email.EmailMessage;
import com.obraia.backend.email.EmailSender;
import com.obraia.backend.entity.User;

/**
 * Writes the account emails. The links open pages of the frontend, which
 * then call the API with the token.
 */
@Service
public class AuthEmailService {

    private static final Logger log = LoggerFactory.getLogger(AuthEmailService.class);

    private final EmailSender emailSender;
    private final String frontendBaseUrl;

    public AuthEmailService(EmailSender emailSender, @Value("${app.frontend-url}") String frontendUrls) {
        this.emailSender = emailSender;
        // FRONTEND_URL may list several URLs: the first one is used in the links.
        String first = frontendUrls.split(",")[0].trim();
        this.frontendBaseUrl = first.endsWith("/") ? first.substring(0, first.length() - 1) : first;
    }

    public void sendVerificationEmail(User user, String token) {
        String link = buildLink("/verify-email", token);
        String html = """
                <p>Hi %s,</p>
                <p>Thanks for signing up for ObraIA. Confirm your email to activate your account:</p>
                <p><a href="%s">Verify my email</a></p>
                <p>The link expires in 24 hours. If you did not create an account, ignore this email.</p>
                """.formatted(escape(user.getFirstName()), link);
        deliver(new EmailMessage(user.getEmail(), fullName(user), "Verify your ObraIA account", html));
    }

    public void sendPasswordResetEmail(User user, String token) {
        String link = buildLink("/reset-password", token);
        String html = """
                <p>Hi %s,</p>
                <p>We received a request to reset your ObraIA password:</p>
                <p><a href="%s">Choose a new password</a></p>
                <p>The link expires in 30 minutes. If you did not ask for it, ignore this email.</p>
                """.formatted(escape(user.getFirstName()), link);
        deliver(new EmailMessage(user.getEmail(), fullName(user), "Reset your ObraIA password", html));
    }

    String buildLink(String path, String token) {
        return frontendBaseUrl + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * A failed email does not cancel the operation: the account is already
     * saved and the user can ask for a new link.
     */
    private void deliver(EmailMessage message) {
        try {
            emailSender.send(message);
        } catch (EmailDeliveryException ex) {
            log.error("Could not send \"{}\" to {}", message.subject(), message.to(), ex);
        }
    }

    private static String fullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    /** The name is typed by the user, so it is escaped before going into HTML. */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
