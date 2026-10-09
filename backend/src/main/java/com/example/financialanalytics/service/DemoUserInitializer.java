package com.example.financialanalytics.service;

import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DemoUserInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoUserInitializer.class);

    private final boolean enabled;
    private final String name;
    private final String email;
    private final String password;
    private final UserRepository userRepository;
    private final FinancialDataGenerator financialDataGenerator;
    private final BCryptPasswordEncoder passwordEncoder;

    public DemoUserInitializer(
            @Value("${app.demo-user.enabled:false}") boolean enabled,
            @Value("${app.demo-user.name:Demo Customer}") String name,
            @Value("${app.demo-user.email:demo@fintrack.local}") String email,
            @Value("${app.demo-user.password:}") String password,
            UserRepository userRepository,
            FinancialDataGenerator financialDataGenerator,
            BCryptPasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.name = name;
        this.email = email;
        this.password = password;
        this.userRepository = userRepository;
        this.financialDataGenerator = financialDataGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        if (password.isBlank()) {
            throw new IllegalStateException("DEMO_USER_PASSWORD must be configured when demo-user seeding is enabled");
        }

        User user = userRepository.saveAndFlush(new User(UUID.randomUUID(), name,
                email.trim().toLowerCase(java.util.Locale.ROOT), passwordEncoder.encode(password)));
        financialDataGenerator.createAccountWithSampleData(user.getId());
        log.info("Demo user initialized with persistent sample data: userId={}", user.getId());
    }
}
