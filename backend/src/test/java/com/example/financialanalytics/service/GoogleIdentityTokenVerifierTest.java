package com.example.financialanalytics.service;

import com.example.financialanalytics.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleIdentityTokenVerifierTest {
    @Test
    void reportsGoogleSignInAsUnavailableWhenNoClientIdIsConfigured() {
        GoogleIdentityTokenVerifier verifier = new GoogleIdentityTokenVerifier("");

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("credential"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status());
    }
}
