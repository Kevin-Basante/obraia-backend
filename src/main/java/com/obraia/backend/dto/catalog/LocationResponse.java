package com.obraia.backend.dto.catalog;

import java.util.UUID;

import com.obraia.backend.entity.catalog.Location;

public record LocationResponse(UUID id, String city, String department, String country) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(location.getId(), location.getCity(), location.getDepartment(),
                location.getCountry());
    }
}
