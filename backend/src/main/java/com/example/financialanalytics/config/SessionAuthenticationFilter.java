package com.example.financialanalytics.config;

import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

final class SessionAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/google");

    private final AuthService authService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    SessionAuthenticationFilter(AuthService authService, AuthenticationEntryPoint authenticationEntryPoint) {
        this.authService = authService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/v1/")
                || PUBLIC_AUTH_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        User user;
        try {
            user = authService.requireUser(request.getHeader("Authorization"));
        } catch (ApiException exception) {
            if (exception.status().value() == HttpServletResponse.SC_UNAUTHORIZED) {
                authenticationEntryPoint.commence(request, response,
                        new InsufficientAuthenticationException(exception.getMessage(), exception));
                return;
            }
            throw exception;
        }
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return request.getRequestURI().substring(contextPath.length());
    }
}
