package com.obraia.backend.entity.project;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.obraia.backend.entity.catalog.Location;
import com.obraia.backend.entity.catalog.ProjectType;
import com.obraia.backend.entity.catalog.SizeCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A construction project. Deleting it only fills deleted_at; the row is
 * removed for good 30 days later.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_type_id", nullable = false)
    private ProjectType projectType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_manager_id")
    private SiteManager siteManager;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(name = "built_area_m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal builtAreaM2;

    @Column(nullable = false)
    private short floors;

    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    // Filled by the database (default now() and the updated_at trigger).
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Project() {
        // Required by JPA
    }

    public Project(UUID createdBy, ProjectType projectType, Location location, String name, String address,
            BigDecimal builtAreaM2, int floors, LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.createdBy = createdBy;
        apply(projectType, location, name, address, builtAreaM2, floors, plannedStartDate, plannedEndDate);
    }

    // Not final on purpose: Hibernate may hand out a proxy of this class, and
    // a final method would write to the proxy instead of the real entity.
    public void update(ProjectType projectType, Location location, String name, String address,
            BigDecimal builtAreaM2, int floors, LocalDate plannedStartDate, LocalDate plannedEndDate) {
        apply(projectType, location, name, address, builtAreaM2, floors, plannedStartDate, plannedEndDate);
    }

    private void apply(ProjectType projectType, Location location, String name, String address,
            BigDecimal builtAreaM2, int floors, LocalDate plannedStartDate, LocalDate plannedEndDate) {
        this.projectType = projectType;
        this.location = location;
        this.name = name;
        this.address = address;
        this.builtAreaM2 = builtAreaM2;
        this.floors = (short) floors;
        this.plannedStartDate = plannedStartDate;
        this.plannedEndDate = plannedEndDate;
    }

    /**
     * The database also computes size_category from the area. It is
     * calculated here too so it is right before the row is read again.
     */
    public SizeCategory getSizeCategory() {
        return SizeCategory.forArea(builtAreaM2);
    }

    public void assignSiteManager(SiteManager siteManager) {
        this.siteManager = siteManager;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public Location getLocation() {
        return location;
    }

    public SiteManager getSiteManager() {
        return siteManager;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getBuiltAreaM2() {
        return builtAreaM2;
    }

    public int getFloors() {
        return floors;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public LocalDate getPlannedStartDate() {
        return plannedStartDate;
    }

    public LocalDate getPlannedEndDate() {
        return plannedEndDate;
    }

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
