package com.example.financialanalytics.service;

import com.example.financialanalytics.exception.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Component
public class GoogleIdentityTokenVerifier {
    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityTokenVerifier.class);
    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId.trim();
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(this.clientId.isEmpty() ? "google-sign-in-disabled" : this.clientId))
                .build();
    }

    public Identity verify(String credential) {
        if (clientId.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Google sign-in is not configured");
        }
        try {
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Google credential is invalid or expired");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            Object nameClaim = payload.get("name");
            if (subject == null || subject.isBlank() || email == null || email.isBlank()
                    || subject.length() > 255 || email.length() > 254
                    || !Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new ApiException(HttpStatus.UNAUTHORIZED,
                        "Google account must provide a verified email address");
            }
            String name = nameClaim instanceof String value && !value.isBlank() ? value : email;
            return new Identity(subject, email, name);
        } catch (IOException | GeneralSecurityException exception) {
            log.warn("Google credential verification unavailable: {}", exception.getClass().getSimpleName());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google sign-in could not be verified; please try again");
        }
    }

    public record Identity(String subject, String email, String name) {}
}
