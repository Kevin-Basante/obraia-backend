package com.obraia.backend.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Platform or project role. The rows are loaded by the seed migration (V3). */
@Entity
@Table(name = "roles")
public class Role {

    /** Role given to every account created with the register endpoint. */
    public static final String USER = "user";

    /** Project role of the person who builds on their own land. Has every permission in the project. */
    public static final String OWNER = "owner";

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 10)
    private String scope;

    protected Role() {
        // Required by JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getScope() {
        return scope;
    }
}
