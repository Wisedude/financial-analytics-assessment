package com.example.financialanalytics.service;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.Account;
import com.example.financialanalytics.model.Budget;
import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.BudgetRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetService {
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public BudgetService(AccountRepository accountRepository, BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.BudgetResponse> getBudgets(User user) {
        UUID accountId = requireAccount(user).getId();
        Map<String, BigDecimal> spend = monthSpending(accountId);
        return budgetRepository.findByAccountIdOrderByCategory(accountId).stream()
                .map(budget -> response(budget, spend.getOrDefault(budget.getCategory(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public AnalyticsDtos.BudgetResponse updateBudget(User user, String category, BigDecimal amount) {
        UUID accountId = requireAccount(user).getId();
        Budget budget = budgetRepository.findByAccountIdAndCategoryIgnoreCase(accountId, category.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Budget category not found"));
        budget.updateAmount(amount.setScale(2, RoundingMode.HALF_UP));
        BigDecimal currentSpend = monthSpending(accountId).getOrDefault(budget.getCategory(), BigDecimal.ZERO);
        return response(budgetRepository.save(budget), currentSpend);
    }

    private Account requireAccount(User user) {
        return accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private Map<String, BigDecimal> monthSpending(UUID accountId) {
        LocalDate start = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return transactionRepository.findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        accountId, start, end).stream()
                .filter(transaction -> "DEBIT".equals(transaction.getType()))
                .collect(Collectors.groupingBy(FinancialTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO,
                                transaction -> transaction.getAmount().abs(), BigDecimal::add)));
    }

    private AnalyticsDtos.BudgetResponse response(Budget budget, BigDecimal spent) {
        BigDecimal amount = spent.setScale(2, RoundingMode.HALF_UP);
        double percentage = budget.getAmount().signum() == 0 ? 0
                : amount.multiply(BigDecimal.valueOf(100))
                .divide(budget.getAmount(), 2, RoundingMode.HALF_UP).doubleValue();
        return new AnalyticsDtos.BudgetResponse(budget.getCategory(), amount, budget.getAmount(), percentage);
    }
}
