package com.example.financialanalytics.service;

import com.example.financialanalytics.dto.AuthDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.AuthSession;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.AuthSessionRepository;
import com.example.financialanalytics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final FinancialDataGenerator financialDataGenerator;
    private final BCryptPasswordEncoder passwordEncoder;
    private final GoogleIdentityTokenVerifier googleTokenVerifier;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;
    private final long tokenExpiryMinutes;

    public AuthService(UserRepository userRepository,
                       AuthSessionRepository sessionRepository,
                       FinancialDataGenerator financialDataGenerator,
                       GoogleIdentityTokenVerifier googleTokenVerifier,
                       BCryptPasswordEncoder passwordEncoder,
                       Clock clock,
                       @Value("${app.token-expiry-minutes:120}") long tokenExpiryMinutes) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.financialDataGenerator = financialDataGenerator;
        this.googleTokenVerifier = googleTokenVerifier;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.tokenExpiryMinutes = tokenExpiryMinutes;
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        log.info("User authenticated: userId={}", user.getId());
        return issueToken(user);
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User(UUID.randomUUID(), request.name().trim(), email,
                passwordEncoder.encode(request.password()));
        userRepository.saveAndFlush(user);
        financialDataGenerator.createAccountWithSampleData(user.getId());
        log.info("User registered with sample financial data: userId={}", user.getId());
        return issueToken(user);
    }

    @Transactional
    public AuthDtos.AuthResponse loginWithGoogle(String credential) {
        GoogleIdentityTokenVerifier.Identity identity = googleTokenVerifier.verify(credential);
        User user = userRepository.findByGoogleSubject(identity.subject()).orElse(null);
        if (user == null) {
            String email = identity.email().trim().toLowerCase(Locale.ROOT);
            if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "This email already has a FinTrack account. Sign in with your password, then link Google in Settings.");
            }
            byte[] randomPassword = new byte[32];
            secureRandom.nextBytes(randomPassword);
            String passwordHash = passwordEncoder.encode(Base64.getUrlEncoder()
                    .withoutPadding().encodeToString(randomPassword));
            String name = identity.name().trim();
            if (name.length() > 80) name = name.substring(0, 80);
            user = new User(UUID.randomUUID(), name, email, passwordHash);
            user.setGoogleSubject(identity.subject());
            userRepository.saveAndFlush(user);
            financialDataGenerator.createAccountWithSampleData(user.getId());
            log.info("User registered with Google and sample financial data: userId={}", user.getId());
        } else {
            log.info("User authenticated with Google: userId={}", user.getId());
        }
        return issueToken(user);
    }

    @Transactional
    public void linkGoogleIdentity(UUID userId, String credential) {
        GoogleIdentityTokenVerifier.Identity identity = googleTokenVerifier.verify(credential);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        if (!user.getEmail().equalsIgnoreCase(identity.email().trim())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Sign in to Google with the same email address as your FinTrack account");
        }
        User owner = userRepository.findByGoogleSubject(identity.subject()).orElse(null);
        if (owner != null && !owner.getId().equals(userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This Google account is linked to another FinTrack account");
        }
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject())) {
            throw new ApiException(HttpStatus.CONFLICT, "A different Google account is already linked");
        }
        user.setGoogleSubject(identity.subject());
        userRepository.save(user);
        log.info("Google identity linked: userId={}", user.getId());
    }

    @Transactional(readOnly = true)
    public User requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.length() <= 7) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String tokenHash = hashToken(authorization.substring(7));
        AuthSession session = sessionRepository.findById(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
        if (!session.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
    }

    @Transactional
    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")
                && authorization.length() > 7) {
            sessionRepository.deleteById(hashToken(authorization.substring(7)));
        }
    }

    private AuthDtos.AuthResponse issueToken(User user) {
        sessionRepository.deleteByExpiresAtBefore(Instant.now(clock));
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now(clock).plus(tokenExpiryMinutes, ChronoUnit.MINUTES);
        sessionRepository.save(new AuthSession(hashToken(token), user.getId(), expiresAt));
        return new AuthDtos.AuthResponse(token,
                new AuthDtos.UserResponse(user.getId().toString(), user.getName(), user.getEmail()));
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
