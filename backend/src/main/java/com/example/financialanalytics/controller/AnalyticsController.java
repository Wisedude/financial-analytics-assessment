package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.service.AnalyticsService;
import com.example.financialanalytics.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AuthService authService;
    private final AnalyticsService analyticsService;

    public AnalyticsController(AuthService authService, AnalyticsService analyticsService) {
        this.authService = authService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public AnalyticsDtos.DashboardResponse dashboard(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return analyticsService.getDashboard(authService.requireUser(authorization));
    }

    @GetMapping("/cash-flow")
    public List<AnalyticsDtos.CashFlowPoint> cashFlow(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return analyticsService.getCashFlow(authService.requireUser(authorization));
    }

    @GetMapping("/spending")
    public List<AnalyticsDtos.CategorySpend> spending(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return analyticsService.getSpending(authService.requireUser(authorization));
    }
}
