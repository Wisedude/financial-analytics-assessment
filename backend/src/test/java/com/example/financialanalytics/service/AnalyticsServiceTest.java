package com.example.financialanalytics.service;

import com.example.financialanalytics.model.Account;
import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.BudgetRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {
    private final UUID userId = UUID.fromString("36c0b559-b76c-402b-9c7d-f5656382460c");
    private final UUID accountId = UUID.fromString("4d2bf2ca-c37e-486b-8b70-a45b35eac77e");
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final AnalyticsService service =
            new AnalyticsService(accountRepository, transactionRepository, budgetRepository);
    private final User user = new User(userId, "Test User", "test@example.com", "password-hash");
    private final List<FinancialTransaction> transactions = List.of(
            transaction("2026-10-02", "Salary", "Income", "5000.00", "CREDIT"),
            transaction("2026-10-04", "Grocer", "Groceries", "-250.00", "DEBIT"),
            transaction("2026-09-02", "Salary", "Income", "4800.00", "CREDIT"),
            transaction("2026-09-04", "Rent", "Housing", "-1400.00", "DEBIT"));

    @BeforeEach
    void setUp() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(
                new Account(accountId, userId, "ZAR", new BigDecimal("10000.00"))));
        when(transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId))
                .thenReturn(transactions);
        when(transactionRepository.sumAmountByAccountId(accountId)).thenReturn(new BigDecimal("8150.00"));
        when(budgetRepository.findByAccountIdOrderByCategory(accountId)).thenReturn(List.of());
    }

    @Test
    void filtersSummaryAndCashFlowForSelectedMonthAndListsMonthsFromAccountData() {
        var dashboard = service.getDashboard(user, "2026-10");

        assertEquals("2026-10", dashboard.selectedMonth());
        assertEquals("October 2026", dashboard.summary().period());
        assertEquals(new BigDecimal("5000.00"), dashboard.summary().income());
        assertEquals(new BigDecimal("250.00"), dashboard.summary().spending());
        assertEquals(1, dashboard.cashFlow().size());
        assertEquals(List.of("2026-10", "2026-09"),
                dashboard.availableMonths().stream().map(option -> option.value()).toList());
        assertEquals(2, dashboard.transactions().size());
    }

    @Test
    void allSelectionAggregatesEveryTransactionAndCashFlowMonth() {
        var dashboard = service.getDashboard(user, "all");

        assertEquals("all", dashboard.selectedMonth());
        assertEquals("All time", dashboard.summary().period());
        assertEquals(new BigDecimal("9800.00"), dashboard.summary().income());
        assertEquals(new BigDecimal("1650.00"), dashboard.summary().spending());
        assertEquals(2, dashboard.cashFlow().size());
        assertEquals(4, dashboard.transactions().size());
    }

    @Test
    void defaultsToTheLatestMonthAvailableInTheAccount() {
        var dashboard = service.getDashboard(user);

        assertEquals("2026-10", dashboard.selectedMonth());
        assertEquals(2, dashboard.transactions().size());
    }

    @Test
    void rejectsMalformedAndUnavailableMonths() {
        assertThrows(ApiException.class, () -> service.getDashboard(user, "2026-13"));
        assertThrows(ApiException.class, () -> service.getDashboard(user, "2026-08"));
    }

    private FinancialTransaction transaction(String date, String merchant, String category,
                                             String amount, String type) {
        return new FinancialTransaction(UUID.randomUUID(), accountId, LocalDate.parse(date),
                merchant, category, new BigDecimal(amount), type);
    }
}
