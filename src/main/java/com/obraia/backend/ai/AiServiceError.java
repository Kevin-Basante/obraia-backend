package com.obraia.backend.ai;

/**
 * Error answer of obraia-ia: { "error": { "code", "message" } }, the same
 * shape the backend uses. Only the adapter reads it.
 */
record AiServiceError(Body error) {

    record Body(String code, String message) {
    }
}
