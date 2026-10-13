package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AuthDtos;
import com.example.financialanalytics.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        AuthDtos.AuthResponse response = authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/account"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthDtos.AuthResponse> googleLogin(
            @Valid @RequestBody AuthDtos.GoogleLoginRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(authService.loginWithGoogle(request.credential()));
    }

    @PostMapping("/google/link")
    public ResponseEntity<Void> linkGoogle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AuthDtos.GoogleLoginRequest request) {
        authService.linkGoogleIdentity(authService.requireUser(authorization).getId(), request.credential());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value="Authorization", required=false) String authorization) {
        authService.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
