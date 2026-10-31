package com.obraia.backend.dto.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import com.obraia.backend.entity.catalog.Location;

/** A city and the transport percentage its quotations add to the materials. */
public record LocationResponse(UUID id, String city, String department, String country, BigDecimal transportPct) {

    public static LocationResponse from(Location location) {
        return new LocationResponse(location.getId(), location.getCity(), location.getDepartment(),
                location.getCountry(), location.getTransportPct());
    }
}
