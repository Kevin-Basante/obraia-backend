package com.obraia.backend.dto.ai;

/**
 * Answer of the wake-up of the AI service; the wake-up itself goes on in the background.
 *
 * @param enabled false when the AI service is not configured, so nothing is woken
 * @param url     health check of the AI service, for the browser to visit: Render only
 *                wakes the service for calls from outside it. Null when it is not configured.
 */
public record AiWakeResponse(boolean enabled, String url) {
}
