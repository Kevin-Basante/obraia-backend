package com.obraia.backend.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Every error has the same JSON shape:
 * {@code { "error": { "code": "...", "message": "..." } }}
 * Validation errors also include "fields" with the problem of each field.
 */
public record ErrorResponse(ErrorDetail error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(String code, String message, Map<String, String> fields) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message, null));
    }

    public static ErrorResponse withFields(String code, String message, Map<String, String> fields) {
        return new ErrorResponse(new ErrorDetail(code, message, fields));
    }
}
