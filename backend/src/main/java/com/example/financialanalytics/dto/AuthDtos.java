package com.example.financialanalytics.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@Email @NotBlank String email,
                               @NotBlank @Size(max=72) String password) {}
    public record RegisterRequest(@NotBlank @Size(min=2, max=80) String name,
                                  @Email @NotBlank String email,
                                  @NotBlank @Size(min=8, max=72) String password) {}
    public record GoogleLoginRequest(@NotBlank @Size(max=8192) String credential) {}
    public record AuthResponse(String token, UserResponse user) {}
    public record UserResponse(String id, String name, String email) {}
}
