package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public AnalyticsDtos.DashboardResponse dashboard(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String month) {
        return month == null ? analyticsService.getDashboard(user)
                : analyticsService.getDashboard(user, month);
    }

    @GetMapping("/cash-flow")
    public List<AnalyticsDtos.CashFlowPoint> cashFlow(@AuthenticationPrincipal User user) {
        return analyticsService.getCashFlow(user);
    }

    @GetMapping("/spending")
    public List<AnalyticsDtos.CategorySpend> spending(@AuthenticationPrincipal User user) {
        return analyticsService.getSpending(user);
    }
}
