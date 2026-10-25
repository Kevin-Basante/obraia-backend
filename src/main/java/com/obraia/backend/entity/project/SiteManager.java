package com.obraia.backend.entity.project;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Site manager ("maestro de obra"). It is a contact saved by a user, not
 * an account: site managers do not sign in.
 */
@Entity
@Table(name = "site_managers")
public class SiteManager {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** User who saved the contact; only that user can see and use it. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(length = 20)
    private String phone;

    protected SiteManager() {
        // Required by JPA
    }

    public SiteManager(UUID createdBy, String fullName, String documentNumber, String phone) {
        this.createdBy = createdBy;
        apply(fullName, documentNumber, phone);
    }

    // Not final, for the same reason as Project.update.
    public void update(String fullName, String documentNumber, String phone) {
        apply(fullName, documentNumber, phone);
    }

    private void apply(String fullName, String documentNumber, String phone) {
        this.fullName = fullName;
        this.documentNumber = documentNumber;
        this.phone = phone;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public String getPhone() {
        return phone;
    }
}
