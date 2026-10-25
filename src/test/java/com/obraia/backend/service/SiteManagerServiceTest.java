package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.project.SiteManagerRequest;
import com.obraia.backend.dto.project.SiteManagerResponse;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.SiteManagerRepository;

class SiteManagerServiceTest {

    private final UUID userId = UUID.randomUUID();

    private SiteManagerRepository repository;
    private SiteManagerService service;

    @BeforeEach
    void setUp() {
        repository = mock(SiteManagerRepository.class);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        service = new SiteManagerService(repository);
    }

    @Test
    void blankOptionalFieldsAreSavedAsEmpty() {
        SiteManagerResponse response = service.create(userId, new SiteManagerRequest(" Pedro Gomez ", " ", ""));

        assertEquals("Pedro Gomez", response.fullName());
        assertNull(response.documentNumber());
        assertNull(response.phone());
    }

    @Test
    void siteManagerOfAnotherUserIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndCreatedBy(id, userId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.update(id, userId, new SiteManagerRequest("Pedro Gomez", null, null)));

        assertEquals("SITE_MANAGER_NOT_FOUND", ex.getCode());
    }
}
