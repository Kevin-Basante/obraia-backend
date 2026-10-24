package com.obraia.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development sender: writes the email to the log instead of sending it,
 * so the verification and reset links can be copied from the console.
 */
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.info("Email not sent (MAIL_PROVIDER=log). To: {} | Subject: {}\n{}",
                message.to(), message.subject(), message.htmlContent());
    }
}
