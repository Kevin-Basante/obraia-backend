package com.obraia.backend.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Every error has the same JSON shape:
 * {@code { "error": { "code": "...", "message": "..." } }}
 * Validation errors also include "fields" with the problem of each field,
 * and some errors include "params" with the values of the message.
 */
public record ErrorResponse(ErrorDetail error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetail(String code, String message, Map<String, String> fields, Map<String, String> params) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message, null, null));
    }

    public static ErrorResponse withParams(String code, String message, Map<String, String> params) {
        return new ErrorResponse(new ErrorDetail(code, message, null, params.isEmpty() ? null : params));
    }

    public static ErrorResponse withFields(String code, String message, Map<String, String> fields) {
        return new ErrorResponse(new ErrorDetail(code, message, fields, null));
    }
}
