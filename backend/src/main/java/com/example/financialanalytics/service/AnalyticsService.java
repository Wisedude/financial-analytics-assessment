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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final Clock clock;

    public AnalyticsService(AccountRepository accountRepository,
                            TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository,
                            Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.DashboardResponse getDashboard(User user) {
        Account account = requireAccount(user);
        LocalDate today = LocalDate.now(clock);
        LocalDate currentMonth = today.withDayOfMonth(1);
        LocalDate historyStart = currentMonth.minusMonths(5);
        List<FinancialTransaction> history = transactionRepository
                .findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        account.getId(), historyStart, today);
        List<FinancialTransaction> currentMonthTransactions = history.stream()
                .filter(transaction -> !transaction.getTransactionDate().isBefore(currentMonth))
                .toList();

        BigDecimal income = sumCredits(currentMonthTransactions);
        BigDecimal spending = sumDebits(currentMonthTransactions);
        BigDecimal savings = income.subtract(spending).setScale(2, RoundingMode.HALF_UP);
        double savingsRate = income.signum() == 0 ? 0
                : savings.multiply(BigDecimal.valueOf(100))
                .divide(income, 2, RoundingMode.HALF_UP).doubleValue();
        BigDecimal balance = account.getOpeningBalance()
                .add(transactionRepository.sumAmountByAccountId(account.getId()))
                .setScale(2, RoundingMode.HALF_UP);

        List<AnalyticsDtos.CategorySpend> categories = categorySpending(account.getId(),
                currentMonthTransactions);
        List<AnalyticsDtos.CashFlowPoint> cashFlow = cashFlow(history, historyStart, currentMonth);
        List<AnalyticsDtos.Insight> insights = insights(savingsRate, categories, spending);
        List<AnalyticsDtos.TransactionResponse> transactions = history.stream()
                .limit(50).map(this::toResponse).toList();
        String period = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + currentMonth.getYear();
        var summary = new AnalyticsDtos.Summary(account.getCurrency(), income, spending,
                savings, savingsRate, balance, period);
        return new AnalyticsDtos.DashboardResponse(summary, cashFlow, categories, transactions, insights);
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.CashFlowPoint> getCashFlow(User user) {
        return getDashboard(user).cashFlow();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.CategorySpend> getSpending(User user) {
        return getDashboard(user).categories();
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.AccountResponse getAccount(User user) {
        Account account = requireAccount(user);
        BigDecimal balance = account.getOpeningBalance()
                .add(transactionRepository.sumAmountByAccountId(account.getId()))
                .setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsDtos.AccountResponse(account.getId().toString(), account.getCurrency(), balance);
    }

    private Account requireAccount(User user) {
        return accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private List<AnalyticsDtos.CashFlowPoint> cashFlow(List<FinancialTransaction> transactions,
                                                        LocalDate start, LocalDate currentMonth) {
        Map<LocalDate, List<FinancialTransaction>> byMonth = new HashMap<>();
        transactions.forEach(transaction -> byMonth.computeIfAbsent(
                transaction.getTransactionDate().withDayOfMonth(1), ignored -> new ArrayList<>()).add(transaction));
        List<AnalyticsDtos.CashFlowPoint> points = new ArrayList<>();
        for (LocalDate month = start; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            List<FinancialTransaction> monthTransactions = byMonth.getOrDefault(month, List.of());
            points.add(new AnalyticsDtos.CashFlowPoint(
                    month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    sumCredits(monthTransactions), sumDebits(monthTransactions)));
        }
        return points;
    }

    private List<AnalyticsDtos.CategorySpend> categorySpending(
            UUID accountId, List<FinancialTransaction> transactions) {
        Map<String, BigDecimal> amounts = new HashMap<>();
        transactions.stream().filter(transaction -> "DEBIT".equals(transaction.getType()))
                .forEach(transaction -> amounts.merge(transaction.getCategory(),
                        transaction.getAmount().abs(), BigDecimal::add));
        BigDecimal totalSpending = amounts.values().stream().reduce(ZERO, BigDecimal::add);
        return budgetRepository.findByAccountIdOrderByCategory(accountId).stream()
                .map(budget -> {
                    BigDecimal amount = amounts.getOrDefault(budget.getCategory(), ZERO)
                            .setScale(2, RoundingMode.HALF_UP);
                    double percentage = totalSpending.signum() == 0 ? 0
                            : amount.multiply(BigDecimal.valueOf(100))
                            .divide(totalSpending, 2, RoundingMode.HALF_UP).doubleValue();
                    return new AnalyticsDtos.CategorySpend(budget.getCategory(), amount,
                            budget.getAmount(), percentage);
                })
                .sorted(Comparator.comparing(AnalyticsDtos.CategorySpend::amount).reversed())
                .toList();
    }

    private List<AnalyticsDtos.Insight> insights(double savingsRate,
                                                 List<AnalyticsDtos.CategorySpend> categories,
                                                 BigDecimal spending) {
        List<AnalyticsDtos.Insight> insights = new ArrayList<>();
        categories.stream().filter(category -> category.amount().compareTo(category.budget()) > 0)
                .findFirst().ifPresent(category -> insights.add(new AnalyticsDtos.Insight(
                        "WARNING", category.name() + " is over budget",
                        category.name() + " spending is "
                                + category.amount().subtract(category.budget()).setScale(2, RoundingMode.HALF_UP)
                                + " over your monthly budget.")));
        if (savingsRate >= 20) {
            insights.add(new AnalyticsDtos.Insight("POSITIVE", "Strong savings rate",
                    "You saved " + String.format(Locale.ROOT, "%.1f", savingsRate)
                            + "% of income this period."));
        } else if (spending.signum() > 0) {
            insights.add(new AnalyticsDtos.Insight("INFO", "Review your monthly spending",
                    "Review category spending to find opportunities to save."));
        }
        if (insights.size() < 3 && !categories.isEmpty()) {
            AnalyticsDtos.CategorySpend highest = categories.get(0);
            insights.add(new AnalyticsDtos.Insight("INFO", "Largest spending category",
                    highest.name() + " is your largest category this month."));
        }
        return insights;
    }

    private BigDecimal sumCredits(List<FinancialTransaction> transactions) {
        return transactions.stream().filter(transaction -> "CREDIT".equals(transaction.getType()))
                .map(FinancialTransaction::getAmount).reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumDebits(List<FinancialTransaction> transactions) {
        return transactions.stream().filter(transaction -> "DEBIT".equals(transaction.getType()))
                .map(FinancialTransaction::getAmount).map(BigDecimal::abs).reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public AnalyticsDtos.TransactionResponse toResponse(FinancialTransaction transaction) {
        return new AnalyticsDtos.TransactionResponse(transaction.getId().toString(),
                transaction.getTransactionDate(), transaction.getMerchant(), transaction.getCategory(),
                transaction.getAmount(), transaction.getType());
    }
}
