package com.obraia.backend.email;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.obraia.backend.config.MailProperties;

/** Checks the request sent to Brevo with a fake server: no real email leaves the test. */
class BrevoEmailSenderTest {

    private final EmailMessage message = new EmailMessage("ana@example.com", "Ana Rojas", "Hello", "<p>Hi</p>");

    private MockRestServiceServer server;
    private BrevoEmailSender sender;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new BrevoEmailSender(new MailProperties("brevo", "no-reply@obraia.test", "ObraIA", "test-key"),
                builder);
    }

    @Test
    void sendsTheEmailWithTheApiKeyAndTheSender() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.sender.email").value("no-reply@obraia.test"))
                .andExpect(jsonPath("$.to[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.subject").value("Hello"))
                .andRespond(withStatus(HttpStatus.CREATED));

        sender.send(message);

        server.verify();
    }

    @Test
    void aRejectedEmailBecomesADeliveryError() {
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email")).andRespond(withBadRequest());

        assertThrows(EmailDeliveryException.class, () -> sender.send(message));
    }
}
