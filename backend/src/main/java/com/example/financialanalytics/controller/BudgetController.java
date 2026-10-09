package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.service.AuthService;
import com.example.financialanalytics.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private final AuthService authService;
    private final BudgetService budgetService;

    public BudgetController(AuthService authService, BudgetService budgetService) {
        this.authService = authService;
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<AnalyticsDtos.BudgetResponse> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return budgetService.getBudgets(authService.requireUser(authorization));
    }

    @PutMapping("/{category}")
    public AnalyticsDtos.BudgetResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String category,
            @Valid @RequestBody AnalyticsDtos.UpdateBudgetRequest request) {
        return budgetService.updateBudget(authService.requireUser(authorization),
                category, request.amount());
    }
}
