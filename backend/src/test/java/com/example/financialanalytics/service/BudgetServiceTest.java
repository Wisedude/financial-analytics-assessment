package com.example.financialanalytics.service;

import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.Account;
import com.example.financialanalytics.model.Budget;
import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.BudgetRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final BudgetRepository budgetRepository = mock(BudgetRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final BudgetService service = new BudgetService(accountRepository, budgetRepository,
            transactionRepository,
            Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC));
    private final User user = new User(userId, "Test User", "test@example.com", "password-hash");
    private final Budget groceryBudget =
            new Budget(UUID.randomUUID(), accountId, "Groceries", new BigDecimal("1000.00"));

    @BeforeEach
    void setUp() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(
                new Account(accountId, userId, "ZAR", new BigDecimal("500.00"))));
        when(transactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        transaction("2026-10-02", "Groceries", "-250.25", "DEBIT"),
                        transaction("2026-10-03", "Groceries", "500.00", "CREDIT")));
    }

    @Test
    void returnsMonthlyDebitSpendingAndBudgetPercentages() {
        when(budgetRepository.findByAccountIdOrderByCategory(accountId)).thenReturn(List.of(groceryBudget));

        var response = service.getBudgets(user);

        assertEquals(new BigDecimal("250.25"), response.get(0).amount());
        assertEquals(new BigDecimal("1000.00"), response.get(0).budget());
        assertEquals(25.03, response.get(0).percentage());
    }

    @Test
    void updatesBudgetAndReturnsCurrentSpending() {
        when(budgetRepository.findByAccountIdAndCategoryIgnoreCase(accountId, "Groceries"))
                .thenReturn(Optional.of(groceryBudget));
        when(budgetRepository.save(groceryBudget)).thenReturn(groceryBudget);

        var response = service.updateBudget(user, " Groceries ", new BigDecimal("800.126"));

        assertEquals(new BigDecimal("800.13"), response.budget());
        assertEquals(new BigDecimal("250.25"), response.amount());
        verify(budgetRepository).save(groceryBudget);
    }

    @Test
    void returnsNotFoundForUnknownBudgetCategory() {
        when(budgetRepository.findByAccountIdAndCategoryIgnoreCase(accountId, "Unknown"))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> service.updateBudget(user, "Unknown", BigDecimal.TEN));
    }

    @Test
    void returnsNotFoundWhenUserHasNoAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.getBudgets(user));
    }

    private FinancialTransaction transaction(String date, String category, String amount, String type) {
        return new FinancialTransaction(UUID.randomUUID(), accountId, LocalDate.parse(date),
                "Test merchant", category, new BigDecimal(amount), type);
    }
}
