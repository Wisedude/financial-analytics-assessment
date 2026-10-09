package com.example.financialanalytics.repository;

import com.example.financialanalytics.model.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    void deleteByUserId(UUID userId);
    void deleteByExpiresAtBefore(java.time.Instant instant);
}
