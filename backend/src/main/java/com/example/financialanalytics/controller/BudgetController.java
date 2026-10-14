package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<AnalyticsDtos.BudgetResponse> list(@AuthenticationPrincipal User user) {
        return budgetService.getBudgets(user);
    }

    @PutMapping("/{category}")
    public AnalyticsDtos.BudgetResponse update(
            @AuthenticationPrincipal User user,
            @PathVariable String category,
            @Valid @RequestBody AnalyticsDtos.UpdateBudgetRequest request) {
        return budgetService.updateBudget(user, category, request.amount());
    }
}
