package com.obraia.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.obraia.backend.dto.DatabaseStatus;
import com.obraia.backend.dto.HealthResponse;
import com.obraia.backend.dto.HelloResponse;
import com.obraia.backend.exception.DatabaseUnavailableException;
import com.obraia.backend.repository.HealthRepository;

/** Business logic for the health check and the "Hello world" endpoint. */
@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final HealthRepository healthRepository;
    private final Instant startedAt = Instant.now();

    public HealthService(HealthRepository healthRepository) {
        this.healthRepository = healthRepository;
    }

    /** Never throws: if the database fails, the report says "degraded". */
    public HealthResponse getHealth() {
        DatabaseStatus database;
        long start = System.nanoTime();
        try {
            healthRepository.ping();
            database = DatabaseStatus.connected(elapsedMillis(start));
        } catch (DataAccessException ex) {
            log.warn("Health check could not reach the database: {}", ex.getMessage());
            database = DatabaseStatus.disconnected();
        }

        String status = database.isConnected() ? "ok" : "degraded";
        long uptimeSeconds = Duration.between(startedAt, Instant.now()).toSeconds();
        return new HealthResponse(status, uptimeSeconds, database, OffsetDateTime.now());
    }

    /** Reads real data from the database; throws if the database does not answer. */
    public HelloResponse getHello() {
        long start = System.nanoTime();
        try {
            int tables = healthRepository.countTables();
            List<String> projectTypes = healthRepository.findProjectTypeNames();
            HelloResponse.Database database =
                    new HelloResponse.Database("connected", tables, projectTypes, elapsedMillis(start));
            return new HelloResponse("Hello world", "ObraIA", database, OffsetDateTime.now());
        } catch (DataAccessException ex) {
            throw new DatabaseUnavailableException(ex);
        }
    }

    private static long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }
}
