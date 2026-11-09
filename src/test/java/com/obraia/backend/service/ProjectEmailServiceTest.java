package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.obraia.backend.email.EmailDeliveryException;
import com.obraia.backend.email.EmailMessage;
import com.obraia.backend.email.EmailSender;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.EmailNotSentException;

class ProjectEmailServiceTest {

    private final User user = new User("ana@example.com", "hash", "Ana", "Perez", null);

    private EmailSender sender;
    private ProjectEmailService service;

    @BeforeEach
    void setUp() {
        sender = mock(EmailSender.class);
        service = new ProjectEmailService(sender, "https://obraia-frontend.vercel.app/,http://localhost:5173");
    }

    @Test
    void deletionEmailLinksToTheConfirmationPage() {
        service.sendDeletionConfirmation(user, "Casa <Norte>", "abc_123", Duration.ofMinutes(30),
                Duration.ofDays(30));

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sender).send(message.capture());
        String html = message.getValue().htmlContent();
        assertTrue(html.contains("https://obraia-frontend.vercel.app/confirm-deletion?token=abc_123"));
        assertTrue(html.contains("Casa &lt;Norte&gt;"));
        assertTrue(html.contains("30 minutes"));
        assertEquals("Confirm the deletion of Casa <Norte>", message.getValue().subject());
    }

    @Test
    void deliveryErrorStopsTheRequest() {
        doThrow(new EmailDeliveryException("Brevo is down", null)).when(sender).send(any());

        assertThrows(EmailNotSentException.class, () -> service.sendDeletionConfirmation(user, "Casa", "abc",
                Duration.ofMinutes(30), Duration.ofDays(30)));
    }

    @Test
    void reportApprovalEmailGoesToTheCreatorWithTheLink() {
        User requester = new User("luis@example.com", "hash", "Luis", "Gomez", null);

        service.sendReportApproval(user, requester, "Casa <Norte>", "abc_123", Duration.ofHours(24),
                Duration.ofHours(24));

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(sender).send(message.capture());
        String html = message.getValue().htmlContent();
        assertEquals("ana@example.com", message.getValue().to());
        assertTrue(html.contains("https://obraia-frontend.vercel.app/approve-report?token=abc_123"));
        assertTrue(html.contains("Luis Gomez"));
        assertTrue(html.contains("luis@example.com"));
        assertTrue(html.contains("Casa &lt;Norte&gt;"));
        assertTrue(html.contains("24 hours"));
        assertEquals("Luis Gomez asks for the full report of Casa <Norte>", message.getValue().subject());
    }

    @Test
    void reportApprovalErrorStopsTheRequest() {
        User requester = new User("luis@example.com", "hash", "Luis", "Gomez", null);
        doThrow(new EmailDeliveryException("Brevo is down", null)).when(sender).send(any());

        assertThrows(EmailNotSentException.class, () -> service.sendReportApproval(user, requester, "Casa", "abc",
                Duration.ofHours(24), Duration.ofHours(24)));
    }
}
