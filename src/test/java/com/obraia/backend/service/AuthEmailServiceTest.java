package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.email.EmailDeliveryException;
import com.obraia.backend.email.EmailMessage;
import com.obraia.backend.email.EmailSender;
import com.obraia.backend.entity.User;

class AuthEmailServiceTest {

    private EmailSender sender;
    private AuthEmailService service;
    private final User user = new User("ana@example.com", "hash", "Ana <b>", "Perez", null);

    @BeforeEach
    void setUp() {
        sender = mock(EmailSender.class);
        // Two allowed origins: the links must use the first one, without the final slash.
        service = new AuthEmailService(sender, "https://obraia-frontend.vercel.app/, http://localhost:5173");
    }

    @Test
    void verificationEmailLinksToTheFrontend() {
        service.sendVerificationEmail(user, "abc_123");

        EmailMessage message = captureMessage();
        assertEquals("ana@example.com", message.to());
        assertTrue(message.htmlContent().contains("https://obraia-frontend.vercel.app/verify-email?token=abc_123"));
    }

    @Test
    void resetEmailLinksToTheResetPage() {
        service.sendPasswordResetEmail(user, "xyz");

        assertTrue(captureMessage().htmlContent().contains("/reset-password?token=xyz"));
    }

    @Test
    void userNameIsEscapedInTheHtml() {
        service.sendVerificationEmail(user, "abc");

        String html = captureMessage().htmlContent();
        assertTrue(html.contains("Ana &lt;b&gt;"));
        assertFalse(html.contains("<b>"));
    }

    @Test
    void deliveryErrorDoesNotStopTheOperation() {
        doThrow(new EmailDeliveryException("down", null)).when(sender).send(any());

        assertDoesNotThrow(() -> service.sendVerificationEmail(user, "abc"));
    }

    private EmailMessage captureMessage() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }
}
