package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.catalog.LocationResponse;
import com.obraia.backend.repository.LocationRepository;
import com.obraia.backend.support.CatalogFixtures;

class LocationServiceTest {

    private LocationService service;

    @BeforeEach
    void setUp() {
        LocationRepository repository = mock(LocationRepository.class);
        when(repository.findAllByOrderByCityAsc()).thenReturn(List.of(
                CatalogFixtures.location("Bogotá", "Bogotá D.C."),
                CatalogFixtures.location("Cali", "Valle del Cauca"),
                CatalogFixtures.location("Medellín", "Antioquia")));
        service = new LocationService(repository);
    }

    @Test
    void withoutFiltersReturnsEveryCity() {
        assertEquals(3, service.listLocations(null, null).size());
    }

    @Test
    void departmentFilterIgnoresAccentsAndCase() {
        List<LocationResponse> result = service.listLocations("BOGOTA d.c.", null);

        assertEquals(List.of("Bogotá"), result.stream().map(LocationResponse::city).toList());
    }

    @Test
    void searchFindsPartOfTheCityName() {
        assertEquals("Medellín", service.listLocations(null, "medel").get(0).city());
    }

    @Test
    void departmentsAreSortedWithoutRepeats() {
        assertEquals(List.of("Antioquia", "Bogotá D.C.", "Valle del Cauca"), service.listDepartments());
    }
}
