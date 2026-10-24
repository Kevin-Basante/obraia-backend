package com.obraia.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.obraia.backend.dto.auth.ResetPasswordRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class StrongPasswordTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"secreto123", "A1b2c3d4", "una clave larga 2026"})
    void acceptsPasswordsWithLettersAndNumbers(String password) {
        assertTrue(validator.validate(new ResetPasswordRequest("token", password)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc12", "onlyletters", "12345678"})
    void rejectsWeakPasswords(String password) {
        assertEquals(1, validator.validate(new ResetPasswordRequest("token", password)).size());
    }

    @ParameterizedTest
    @ValueSource(ints = {73, 100})
    void rejectsPasswordsLongerThan72Characters(int length) {
        String password = "a1".repeat(length / 2) + (length % 2 == 0 ? "" : "b");

        assertEquals(1, validator.validate(new ResetPasswordRequest("token", password)).size());
    }
}
