package com.obraia.backend.dto.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ProjectRequestTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 2);

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

    @Test
    void validRequestHasNoErrors() {
        assertTrue(validator.validate(request("120.50", 2, START, START.plusDays(90))).isEmpty());
    }

    @Test
    void sameStartAndEndDayIsAccepted() {
        assertTrue(validator.validate(request("120", 1, START, START)).isEmpty());
    }

    @Test
    void endBeforeStartIsReportedWithItsOwnKey() {
        assertEquals(Set.of("plannedDatesInOrder"), errorFields(request("120", 1, START, START.minusDays(1))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.50", "2000.01", "120.555"})
    void areaOutOfRangeIsRejected(String area) {
        assertEquals(Set.of("builtAreaM2"), errorFields(request(area, 1, null, null)));
    }

    @Test
    void onlyOneOrTwoFloorsAreAllowed() {
        assertEquals(Set.of("floors"), errorFields(request("120", 3, null, null)));
    }

    private static ProjectRequest request(String area, int floors, LocalDate start, LocalDate end) {
        return new ProjectRequest("Casa Prueba", UUID.randomUUID(), UUID.randomUUID(), null, new BigDecimal(area),
                floors, start, end);
    }

    private static Set<String> errorFields(ProjectRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
