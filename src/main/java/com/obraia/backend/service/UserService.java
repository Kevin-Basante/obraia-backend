package com.obraia.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.user.UserResponse;
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
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "The user does not exist"));
    }
}
