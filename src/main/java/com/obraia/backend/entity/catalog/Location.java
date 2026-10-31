package com.obraia.backend.entity.catalog;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** City where a project is built. Quotations use it for transport costs. */
@Entity
@Table(name = "locations")
public class Location {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 60)
    private String country;

    /** Cost of bringing the materials to the site, as a percentage of their price. */
    @Column(name = "transport_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal transportPct;

    protected Location() {
        // Required by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public String getDepartment() {
        return department;
    }

    public String getCountry() {
        return country;
    }

    public BigDecimal getTransportPct() {
        return transportPct;
    }
}
