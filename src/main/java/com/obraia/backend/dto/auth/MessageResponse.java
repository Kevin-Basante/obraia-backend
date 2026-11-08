package com.obraia.backend.dto.auth;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Simple answer with a message that the frontend can show. When the message
 * has a code, the frontend writes it in the language of the user with the
 * params; the English message stays for other clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(String message, String code, Map<String, String> params) {

    public MessageResponse(String message) {
        this(message, null, null);
    }

    public static MessageResponse of(String code, String message, Map<String, String> params) {
        return new MessageResponse(message, code, params.isEmpty() ? null : Map.copyOf(params));
    }
}
