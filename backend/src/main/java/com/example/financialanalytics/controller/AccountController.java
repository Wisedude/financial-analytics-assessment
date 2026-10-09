package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.dto.AuthDtos;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AnalyticsService;
import com.example.financialanalytics.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final AuthService authService;
    private final AnalyticsService analyticsService;

    public AccountController(AuthService authService, AnalyticsService analyticsService) {
        this.authService = authService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AccountDetails get(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authService.requireUser(authorization);
        return new AccountDetails(new AuthDtos.UserResponse(user.getId().toString(),
                user.getName(), user.getEmail()), analyticsService.getAccount(user));
    }

    public record AccountDetails(AuthDtos.UserResponse user, AnalyticsDtos.AccountResponse account) {}
}
