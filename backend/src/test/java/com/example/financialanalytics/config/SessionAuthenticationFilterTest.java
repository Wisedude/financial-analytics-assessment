package com.example.financialanalytics.config;

import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionAuthenticationFilterTest {
    private final AuthService authService = mock(AuthService.class);
    private final ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(new ObjectMapper());
    private final SessionAuthenticationFilter filter = new SessionAuthenticationFilter(authService, entryPoint);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesProtectedRequestAndProvidesUserToDownstreamFilters() throws Exception {
        User user = new User(UUID.randomUUID(), "Test User", "test@example.com", "hash");
        when(authService.requireUser("Bearer valid-token")).thenReturn(user);
        MockHttpServletRequest request = request("/api/v1/transactions");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Object> authenticatedPrincipal = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                authenticatedPrincipal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal()));

        assertEquals(user, authenticatedPrincipal.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void returnsUnauthorizedForInvalidSession() throws Exception {
        when(authService.requireUser(null)).thenThrow(
                new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("/api/v1/account"), response, (servletRequest, servletResponse) -> {
            throw new AssertionError("Unauthenticated request must not continue");
        });

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals("{\"message\":\"Authentication required\"}", response.getContentAsString());
    }

    @Test
    void skipsPublicAuthEndpointsAndPreflightRequests() throws Exception {
        MockHttpServletRequest login = request("/api/v1/auth/login");
        MockHttpServletRequest preflight = request("/api/v1/account");
        preflight.setMethod("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(login, response, (servletRequest, servletResponse) -> {});
        filter.doFilter(preflight, response, (servletRequest, servletResponse) -> {});

        verify(authService, never()).requireUser(null);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setServletPath(uri);
        request.setMethod("GET");
        return request;
    }
}
