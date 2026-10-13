package com.example.financialanalytics.service;

import com.example.financialanalytics.dto.AuthDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.AuthSession;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.AuthSessionRepository;
import com.example.financialanalytics.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceGoogleTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthSessionRepository sessionRepository = mock(AuthSessionRepository.class);
    private final FinancialDataGenerator dataGenerator = mock(FinancialDataGenerator.class);
    private final GoogleIdentityTokenVerifier googleVerifier = mock(GoogleIdentityTokenVerifier.class);
    private final BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
    private final AuthService service = new AuthService(userRepository, sessionRepository, dataGenerator,
            googleVerifier, passwordEncoder,
            Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC), 120);
    private final GoogleIdentityTokenVerifier.Identity identity =
            new GoogleIdentityTokenVerifier.Identity("google-subject-123", "person@example.com", "Example Person");

    @BeforeEach
    void setUp() {
        when(googleVerifier.verify("google-credential")).thenReturn(identity);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-random-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsPersistentAccountAndSessionForNewGoogleIdentity() {
        when(userRepository.findByGoogleSubject(identity.subject())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(identity.email())).thenReturn(Optional.empty());

        AuthDtos.AuthResponse response = service.loginWithGoogle("google-credential");

        assertNotNull(response.token());
        assertEquals(identity.email(), response.user().email());
        assertEquals(identity.name(), response.user().name());
        verify(userRepository).saveAndFlush(any(User.class));
        verify(dataGenerator).createAccountWithSampleData(UUID.fromString(response.user().id()));
        verify(sessionRepository).save(any(AuthSession.class));
    }

    @Test
    void signsIntoExistingGoogleIdentityWithoutReseedingData() {
        User user = userWithGoogleIdentity();
        when(userRepository.findByGoogleSubject(identity.subject())).thenReturn(Optional.of(user));

        AuthDtos.AuthResponse response = service.loginWithGoogle("google-credential");

        assertEquals(user.getId().toString(), response.user().id());
        verify(dataGenerator, never()).createAccountWithSampleData(any(UUID.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void refusesToAutoLinkMatchingEmailToPasswordAccount() {
        when(userRepository.findByGoogleSubject(identity.subject())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(identity.email())).thenReturn(
                Optional.of(new User(UUID.randomUUID(), identity.name(), identity.email(), "existing-hash")));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.loginWithGoogle("google-credential"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        verify(dataGenerator, never()).createAccountWithSampleData(any(UUID.class));
    }

    @Test
    void linksGoogleOnlyAfterTheAuthenticatedAccountEmailMatches() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, identity.name(), identity.email(), "existing-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByGoogleSubject(identity.subject())).thenReturn(Optional.empty());

        service.linkGoogleIdentity(userId, "google-credential");

        assertEquals(identity.subject(), user.getGoogleSubject());
        verify(userRepository).save(user);
    }

    @Test
    void refusesLinkWhenGoogleEmailDoesNotMatchTheSignedInAccount() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Other Person", "other@example.com", "existing-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.linkGoogleIdentity(userId, "google-credential"));

        assertEquals(HttpStatus.CONFLICT, exception.status());
        verify(userRepository, never()).save(any(User.class));
    }

    private User userWithGoogleIdentity() {
        User user = new User(UUID.randomUUID(), identity.name(), identity.email(), "password-hash");
        user.setGoogleSubject(identity.subject());
        return user;
    }
}
