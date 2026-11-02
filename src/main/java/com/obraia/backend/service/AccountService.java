package com.obraia.backend.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.user.ChangePasswordRequest;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.FieldValidationException;
import com.obraia.backend.exception.ResourceNotFoundException;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;

/** Security of the signed-in account: changing the password. */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final AuthEmailService emailService;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService, AuditService auditService, AuthEmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    /**
     * Changes the password after checking the current one. Every session is
     * closed, this one too, so the user signs in again with the new password.
     */
    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request, ClientInfo client) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "The user does not exist"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new FieldValidationException(Map.of("currentPassword", "is not correct"));
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new FieldValidationException(Map.of("newPassword", "must be different from the current password"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(userId);
        auditService.record(AuditEvent.PASSWORD_CHANGED, userId, user.getEmail(), client);
        emailService.sendPasswordChangedEmail(user);
        return new MessageResponse("Your password was changed. Sign in again with the new password.");
    }
}
