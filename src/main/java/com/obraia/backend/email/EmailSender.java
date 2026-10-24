package com.obraia.backend.email;

/**
 * Sends an email. The rest of the application only knows this interface,
 * so the provider can change without touching the services.
 */
public interface EmailSender {

    /** @throws EmailDeliveryException if the provider rejects the email */
    void send(EmailMessage message);
}
