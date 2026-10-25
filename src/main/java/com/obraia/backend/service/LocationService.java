package com.obraia.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.catalog.LocationResponse;
import com.obraia.backend.entity.catalog.Location;
import com.obraia.backend.repository.LocationRepository;
import com.obraia.backend.util.TextSearch;

/**
 * Cities where projects can be built. The list is short and cached, so
 * the filters run in memory instead of in SQL.
 */
@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /** Cities sorted by name. Both filters are optional and ignore accents. */
    @Transactional(readOnly = true)
    public List<LocationResponse> listLocations(String department, String search) {
        return locationRepository.findAllByOrderByCityAsc().stream()
                .map(LocationResponse::from)
                .filter(location -> department == null || department.isBlank()
                        || TextSearch.normalize(location.department()).equals(TextSearch.normalize(department)))
                .filter(location -> TextSearch.matches(location.city(), search))
                .toList();
    }

    /** Departments that have at least one city, sorted by name. */
    @Transactional(readOnly = true)
    public List<String> listDepartments() {
        return locationRepository.findAllByOrderByCityAsc().stream()
                .map(Location::getDepartment)
                .distinct()
                .sorted()
                .toList();
    }
}
