package com.example.financialanalytics.service;

import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoUserInitializerTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FinancialDataGenerator dataGenerator = mock(FinancialDataGenerator.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ApplicationArguments arguments = mock(ApplicationArguments.class);

    @Test
    void skipsWhenDisabledOrDemoUserAlreadyExists() {
        initializer(false, "password").run(arguments);
        verify(userRepository, never()).findByEmailIgnoreCase(any());

        when(userRepository.findByEmailIgnoreCase(" DEMO@EXAMPLE.COM "))
                .thenReturn(Optional.of(new User(UUID.randomUUID(), "Demo", "demo@example.com", "hash")));
        initializer(true, "password").run(arguments);

        verify(dataGenerator, never()).createAccountWithSampleData(any());
    }

    @Test
    void requiresPasswordWhenSeedingIsEnabled() {
        when(userRepository.findByEmailIgnoreCase("demo@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> initializer(true, " ").run(arguments));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsNormalizedDemoUserAndPersistentSampleData() {
        when(userRepository.findByEmailIgnoreCase(" DEMO@EXAMPLE.COM "))
                .thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer(true, "password").run(arguments);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("demo@example.com", saved.getEmail());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("password", saved.getPasswordHash()));
        verify(dataGenerator).createAccountWithSampleData(saved.getId());
    }

    private DemoUserInitializer initializer(boolean enabled, String password) {
        return new DemoUserInitializer(enabled, "Demo User", " DEMO@EXAMPLE.COM ", password,
                userRepository, dataGenerator, passwordEncoder);
    }
}
