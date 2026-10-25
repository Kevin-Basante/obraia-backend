package com.obraia.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.entity.project.Project;
import com.obraia.backend.exception.ConflictException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.ProjectMemberRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.support.ProjectFixtures;
import com.obraia.backend.support.TestUsers;

import jakarta.persistence.EntityManager;

class ProjectMemberServiceTest {

    private final User creator = TestUsers.verifiedUser("hash");
    private final Project project = ProjectFixtures.project(creator.getId(), "Casa Prueba");

    private ProjectMemberRepository members;
    private UserRepository users;
    private ProjectMemberService service;

    @BeforeEach
    void setUp() {
        members = mock(ProjectMemberRepository.class);
        users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        ProjectAccessService access = mock(ProjectAccessService.class);
        when(roles.findByName(Role.OWNER)).thenReturn(Optional.of(ProjectFixtures.ownerRole()));
        when(access.requireProject(project.getId(), creator.getId())).thenReturn(project);
        when(members.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        service = new ProjectMemberService(members, users, roles, access, mock(EntityManager.class));
    }

    @Test
    void verifiedUserIsAddedAsOwner() {
        User partner = TestUsers.verifiedUser("hash");
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(partner));

        assertEquals("owner", service.add(project.getId(), creator.getId(), " ana@example.com ").role());
    }

    @Test
    void unverifiedAccountsCannotBeAdded() {
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com"))
                .thenReturn(Optional.of(TestUsers.unverifiedUser("hash")));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.add(project.getId(), creator.getId(), "ana@example.com"));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        verify(members, never()).saveAndFlush(any());
    }

    @Test
    void sameUserCannotBeAddedTwice() {
        User partner = TestUsers.verifiedUser("hash");
        when(users.findByEmailIgnoreCaseAndDeletedAtIsNull("ana@example.com")).thenReturn(Optional.of(partner));
        when(members.existsByProjectIdAndUserId(project.getId(), partner.getId())).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.add(project.getId(), creator.getId(), "ana@example.com"));

        assertEquals("ALREADY_MEMBER", ex.getCode());
    }

    @Test
    void creatorCannotBeRemoved() {
        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.remove(project.getId(), creator.getId(), creator.getId()));

        assertEquals("CANNOT_REMOVE_CREATOR", ex.getCode());
    }

    @Test
    void removingSomeoneWhoIsNotAMemberAnswers404() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.remove(project.getId(), creator.getId(), UUID.randomUUID()));

        assertEquals("MEMBER_NOT_FOUND", ex.getCode());
    }
}
