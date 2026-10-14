package com.example.financialanalytics.config;

import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AnalyticsService;
import com.example.financialanalytics.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.example.financialanalytics.controller.AccountController.class)
@Import({SecurityConfiguration.class, WebConfig.class, TimeConfiguration.class, RateLimitService.class})
class SecurityConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void protectsAccountApiWhenNoBearerSessionIsProvided() throws Exception {
        when(authService.requireUser(null)).thenThrow(
                new com.example.financialanalytics.exception.ApiException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required"));

        mockMvc.perform(get("/api/v1/account"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void resolvesAuthenticatedUserAndAllowsProtectedControllerToRun() throws Exception {
        User user = new User(UUID.randomUUID(), "Test User", "test@example.com", "hash");
        when(authService.requireUser("Bearer valid-token")).thenReturn(user);
        when(analyticsService.getAccount(user)).thenReturn(
                new com.example.financialanalytics.dto.AnalyticsDtos.AccountResponse(
                        "account-id", "ZAR", new BigDecimal("10.00")));

        mockMvc.perform(get("/api/v1/account").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.account.id").value("account-id"));
    }

    @Test
    void leavesPublicLoginOutsideSessionAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isNotFound());

        verify(authService, never()).requireUser(null);
    }
}
