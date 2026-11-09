package com.obraia.backend.dto.ai;

/**
 * Answer of the wake-up of the AI service; the wake-up itself goes on in the background.
 *
 * @param enabled false when the AI service is not configured, so nothing is woken
 */
public record AiWakeResponse(boolean enabled) {
}
