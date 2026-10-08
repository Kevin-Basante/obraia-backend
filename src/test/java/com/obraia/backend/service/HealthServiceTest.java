package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.obraia.backend.dto.HealthResponse;
import com.obraia.backend.dto.HelloResponse;
import com.obraia.backend.exception.DatabaseUnavailableException;
import com.obraia.backend.repository.HealthRepository;

/** Tests the service with a fake repository, so no database is needed. */
class HealthServiceTest {

    private HealthRepository repository;
    private HealthService service;

    @BeforeEach
    void setUp() {
        repository = mock(HealthRepository.class);
        service = new HealthService(repository);
    }

    @Test
    void healthIsOkWhenDatabaseAnswers() {
        HealthResponse report = service.getHealth();

        assertEquals("ok", report.status());
        assertEquals("connected", report.database().status());
    }

    @Test
    void healthIsDegradedWhenDatabaseFails() {
        doThrow(new DataAccessResourceFailureException("down")).when(repository).ping();

        HealthResponse report = service.getHealth();

        assertEquals("degraded", report.status());
        assertEquals("disconnected", report.database().status());
        assertNull(report.database().latencyMs());
    }

    @Test
    void helloReturnsDataFromDatabase() {
        when(repository.countTables()).thenReturn(30);
        when(repository.findProjectTypeNames()).thenReturn(List.of("Local comercial", "Vivienda unifamiliar"));

        HelloResponse hello = service.getHello();

        assertEquals("Hello world", hello.message());
        assertEquals(30, hello.database().tables());
        assertEquals(2, hello.database().projectTypes().size());
    }

    @Test
    void helloThrowsWhenDatabaseFails() {
        when(repository.countTables()).thenThrow(new DataAccessResourceFailureException("down"));

        assertThrows(DatabaseUnavailableException.class, () -> service.getHello());
    }
}
