package com.obraia.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.user.ProfileRequest;
import com.obraia.backend.dto.user.UserResponse;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return UserResponse.from(requireUser(userId));
    }

    /** Changes the name and the phone. The email is the sign-in name, so it stays. */
    @Transactional
    public UserResponse updateProfile(UUID userId, ProfileRequest request) {
        User user = requireUser(userId);
        user.updateProfile(request.firstName(), request.lastName(), request.phone());
        return UserResponse.from(user);
    }

    private User requireUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "The user does not exist"));
    }
}
