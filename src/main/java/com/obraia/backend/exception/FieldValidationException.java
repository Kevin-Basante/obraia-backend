package com.obraia.backend.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A rule that needs the database or several fields failed. The answer has
 * the same shape as a form validation error, so the frontend can show the
 * message under each field.
 */
public class FieldValidationException extends BadRequestException {

    private final Map<String, String> fields;

    public FieldValidationException(Map<String, String> fields) {
        super("VALIDATION_ERROR", "Some fields are invalid");
        // Keeps the order of the form.
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
