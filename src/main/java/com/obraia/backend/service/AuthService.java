package com.obraia.backend.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.AuthProperties;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.auth.RegisterRequest;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.EmailVerificationToken;
import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.EmailAlreadyRegisteredException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.EmailVerificationTokenRepository;
import com.obraia.backend.repository.PasswordResetTokenRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.security.JwtService;
import com.obraia.backend.security.TokenHasher;

/** Registration, email verification, sign in, sessions and password recovery. */
@Service
public class AuthService {

    static final String VERIFICATION_SENT =
            "If the account exists and is not verified, we sent a new verification link";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthEmailService authEmailService;
    private final AuditService auditService;
    private final AuthProperties properties;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
            EmailVerificationTokenRepository verificationTokens, PasswordResetTokenRepository resetTokens,
            RefreshTokenService refreshTokenService, JwtService jwtService, PasswordEncoder passwordEncoder,
            AuthEmailService authEmailService, AuditService auditService, AuthProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authEmailService = authEmailService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), blankToNull(request.phone()));
        user.addRole(roleRepository.findByName(Role.USER)
                .orElseThrow(() -> new IllegalStateException("Role 'user' is missing: check the V3 migration")));
        userRepository.save(user);

        sendVerificationLink(user);
        auditService.record(AuditEvent.REGISTER, user.getId(), email, client);
        return new MessageResponse("Account created. Check your email to verify your account.");
    }

    @Transactional
    public MessageResponse verifyEmail(String token, ClientInfo client) {
        Instant now = Instant.now();
        EmailVerificationToken verification = verificationTokens.findByTokenHash(TokenHasher.hash(token))
                .filter(t -> t.isUsable(now))
                .orElseThrow(InvalidTokenException::verificationLink);

        verification.markUsed(now);
        User user = verification.getUser();
        user.markEmailVerified(now);
        auditService.record(AuditEvent.EMAIL_VERIFIED, user.getId(), user.getEmail(), client);
        return new MessageResponse("Your email is verified. You can sign in now.");
    }

    /** Always gives the same answer, so nobody can find out which emails are registered. */
    @Transactional
    public MessageResponse resendVerification(String email) {
        userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(normalizeEmail(email))
                .filter(user -> user.canSignIn() && !user.isEmailVerified())
                .ifPresent(user -> {
                    verificationTokens.invalidateUnusedByUserId(user.getId(), Instant.now());
                    sendVerificationLink(user);
                });
        return new MessageResponse(VERIFICATION_SENT);
    }

    private void sendVerificationLink(User user) {
        String token = TokenHasher.generateToken();
        verificationTokens.save(new EmailVerificationToken(user, TokenHasher.hash(token),
                Instant.now().plus(properties.verificationTokenTtl())));
        authEmailService.sendVerificationEmail(user, token);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
