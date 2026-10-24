package com.obraia.backend.email;

/**
 * An email ready to be sent.
 *
 * @param to          recipient address
 * @param toName      recipient name
 * @param subject     subject line
 * @param htmlContent body in HTML
 */
public record EmailMessage(String to, String toName, String subject, String htmlContent) {
}
