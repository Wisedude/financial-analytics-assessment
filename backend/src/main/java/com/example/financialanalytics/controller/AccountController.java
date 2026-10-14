package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.dto.AuthDtos;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
    private final AnalyticsService analyticsService;

    public AccountController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AccountDetails get(@AuthenticationPrincipal User user) {
        return new AccountDetails(new AuthDtos.UserResponse(user.getId().toString(),
                user.getName(), user.getEmail()), analyticsService.getAccount(user));
    }

    public record AccountDetails(AuthDtos.UserResponse user, AnalyticsDtos.AccountResponse account) {}
}
