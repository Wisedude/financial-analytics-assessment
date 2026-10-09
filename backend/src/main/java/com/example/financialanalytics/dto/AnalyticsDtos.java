package com.example.financialanalytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {
    private AnalyticsDtos() {}

    public record DashboardResponse(
            Summary summary,
            List<CashFlowPoint> cashFlow,
            List<CategorySpend> categories,
            List<TransactionResponse> transactions,
            List<Insight> insights) {}

    public record Summary(String currency, BigDecimal income, BigDecimal spending,
                          BigDecimal savings, double savingsRate, BigDecimal availableBalance,
                          String period) {}

    public record CashFlowPoint(String month, BigDecimal income, BigDecimal spending) {}

    public record CategorySpend(String name, BigDecimal amount, BigDecimal budget,
                                double percentage) {}

    public record TransactionResponse(String id, LocalDate date, String merchant, String category,
                                      BigDecimal amount, String type) {}

    public record Insight(String type, String title, String message) {}

    public record BudgetResponse(String category, BigDecimal amount, BigDecimal budget,
                                 double percentage) {}

    public record AccountResponse(String id, String currency, BigDecimal availableBalance) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements,
                                  int totalPages, boolean last) {}

    public record CreateTransactionRequest(
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.PastOrPresent
            java.time.LocalDate date,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 120)
            String merchant,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 60)
            String category,
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.DecimalMin(value = "0.01")
            @jakarta.validation.constraints.Digits(integer = 12, fraction = 2)
            BigDecimal amount,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Pattern(regexp = "CREDIT|DEBIT")
            String type) {}

    public record UpdateBudgetRequest(
            @jakarta.validation.constraints.NotNull
            @jakarta.validation.constraints.DecimalMin(value = "0.00")
            @jakarta.validation.constraints.Digits(integer = 12, fraction = 2)
            BigDecimal amount) {}
}
