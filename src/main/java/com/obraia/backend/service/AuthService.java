package com.obraia.backend.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obraia.backend.config.AuthProperties;
import com.obraia.backend.dto.auth.AuthResponse;
import com.obraia.backend.dto.auth.LoginRequest;
import com.obraia.backend.dto.auth.MessageResponse;
import com.obraia.backend.dto.auth.RegisterRequest;
import com.obraia.backend.dto.user.UserResponse;
import com.obraia.backend.entity.AuditEvent;
import com.obraia.backend.entity.EmailVerificationToken;
import com.obraia.backend.entity.Role;
import com.obraia.backend.entity.User;
import com.obraia.backend.exception.AccountDisabledException;
import com.obraia.backend.exception.EmailAlreadyRegisteredException;
import com.obraia.backend.exception.EmailNotVerifiedException;
import com.obraia.backend.exception.InvalidCredentialsException;
import com.obraia.backend.exception.InvalidTokenException;
import com.obraia.backend.repository.EmailVerificationTokenRepository;
import com.obraia.backend.repository.PasswordResetTokenRepository;
import com.obraia.backend.repository.RoleRepository;
import com.obraia.backend.repository.UserRepository;
import com.obraia.backend.security.ClientInfo;
import com.obraia.backend.security.JwtService;
import com.obraia.backend.security.JwtService.AccessToken;
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
    // Compared when the email does not exist, so the answer takes the same time.
    private final String dummyPasswordHash;

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
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-password-1");
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

    @Transactional
    public AuthResponse login(LoginRequest request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        Optional<User> found = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email);

        String hash = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !passwordMatches) {
            auditService.recordFailure(AuditEvent.LOGIN_FAILED, found.map(User::getId).orElse(null), email, client);
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!user.canSignIn()) {
            throw new AccountDisabledException();
        }
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        user.setLastLoginAt(Instant.now());
        auditService.record(AuditEvent.LOGIN_SUCCESS, user.getId(), email, client);
        return createSession(user, refreshTokenService.issue(user));
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refresh(String refreshToken, ClientInfo client) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        User user = rotation.user();
        if (!user.canSignIn()) {
            refreshTokenService.revokeAll(user.getId());
            throw new AccountDisabledException();
        }
        auditService.record(AuditEvent.TOKEN_REFRESHED, user.getId(), user.getEmail(), client);
        return createSession(user, rotation.newToken());
    }

    /** Signing out an unknown or already revoked token is not an error. */
    @Transactional
    public void logout(String refreshToken, ClientInfo client) {
        refreshTokenService.revoke(refreshToken)
                .ifPresent(userId -> auditService.record(AuditEvent.LOGOUT, userId, null, client));
    }

    private void sendVerificationLink(User user) {
        String token = TokenHasher.generateToken();
        verificationTokens.save(new EmailVerificationToken(user, TokenHasher.hash(token),
                Instant.now().plus(properties.verificationTokenTtl())));
        authEmailService.sendVerificationEmail(user, token);
    }

    private AuthResponse createSession(User user, String refreshToken) {
        AccessToken accessToken = jwtService.createAccessToken(user);
        return AuthResponse.bearer(accessToken.token(), accessToken.expiresInSeconds(), refreshToken,
                UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
