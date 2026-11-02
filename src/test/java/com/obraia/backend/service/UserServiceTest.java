package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.dto.user.ProfileRequest;
import com.obraia.backend.dto.user.UserResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.support.TestUsers;

class UserServiceTest {

    private final User user = TestUsers.verifiedUser("hash");

    private UserService service;

    @BeforeEach
    void setUp() {
        UserRepository users = mock(UserRepository.class);
        when(users.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        service = new UserService(users);
    }

    @Test
    void updateProfileChangesTheNameAndThePhone() {
        UserResponse response = service.updateProfile(user.getId(),
                new ProfileRequest("  María ", " Gómez", " +57 300 123 4567 "));

        assertEquals("María", response.firstName());
        assertEquals("Gómez", response.lastName());
        assertEquals("+57 300 123 4567", user.getPhone());
        assertEquals("ana@example.com", response.email());
    }

    @Test
    void anEmptyPhoneRemovesIt() {
        user.updateProfile("Ana", "Perez", "3001234567");

        service.updateProfile(user.getId(), new ProfileRequest("Ana", "Perez", ""));

        assertNull(user.getPhone());
    }

    @Test
    void anUnknownUserIs404() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.updateProfile(UUID.randomUUID(), new ProfileRequest("Ana", "Perez", null)));

        assertEquals("USER_NOT_FOUND", ex.getCode());
    }
}
