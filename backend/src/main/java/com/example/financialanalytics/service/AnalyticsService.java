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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public AnalyticsService(AccountRepository accountRepository,
                            TransactionRepository transactionRepository,
                            BudgetRepository budgetRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.DashboardResponse getDashboard(User user) {
        return getDashboard(user, null);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.DashboardResponse getDashboard(User user, String requestedMonth) {
        Account account = requireAccount(user);
        List<FinancialTransaction> allTransactions =
                transactionRepository.findByAccountIdOrderByTransactionDateDesc(account.getId());
        List<AnalyticsDtos.MonthOption> availableMonths = availableMonths(allTransactions);
        String month = requestedMonth;
        if (month == null || month.isBlank()) {
            month = availableMonths.isEmpty() ? "all" : availableMonths.get(0).value();
        }
        boolean allMonths = "all".equalsIgnoreCase(month);
        YearMonth selected = allMonths ? null : parseMonth(month);
        if (!allMonths && availableMonths.stream().noneMatch(option -> option.value().equals(selected.toString()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No transaction data is available for that month");
        }
        List<FinancialTransaction> selectedTransactions = allMonths ? allTransactions
                : allTransactions.stream().filter(transaction ->
                        YearMonth.from(transaction.getTransactionDate()).equals(selected)).toList();

        BigDecimal income = sumCredits(selectedTransactions);
        BigDecimal spending = sumDebits(selectedTransactions);
        BigDecimal savings = income.subtract(spending).setScale(2, RoundingMode.HALF_UP);
        double savingsRate = income.signum() == 0 ? 0
                : savings.multiply(BigDecimal.valueOf(100))
                .divide(income, 2, RoundingMode.HALF_UP).doubleValue();
        BigDecimal balance = account.getOpeningBalance()
                .add(transactionRepository.sumAmountByAccountId(account.getId()))
                .setScale(2, RoundingMode.HALF_UP);

        List<AnalyticsDtos.CategorySpend> categories = categorySpending(account.getId(),
                selectedTransactions);
        List<AnalyticsDtos.CashFlowPoint> cashFlow = allMonths
                ? cashFlow(allTransactions) : cashFlow(selectedTransactions, selected);
        List<AnalyticsDtos.Insight> insights = insights(savingsRate, categories, spending,
                allMonths ? Math.max(cashFlow.size(), 1) : 1);
        List<AnalyticsDtos.TransactionResponse> transactions = selectedTransactions.stream()
                .map(this::toResponse).toList();
        String period = allMonths ? "All time" : monthLabel(selected);
        var summary = new AnalyticsDtos.Summary(account.getCurrency(), income, spending,
                savings, savingsRate, balance, period);
        log.info("Dashboard data loaded: period={} transactionCount={}",
                allMonths ? "all" : selected, selectedTransactions.size());
        return new AnalyticsDtos.DashboardResponse(summary, cashFlow, categories, transactions, insights,
                availableMonths, allMonths ? "all" : selected.toString());
    }

    @Transactional(readOnly = true)
    public List<AnalyticsDtos.CashFlowPoint> getCashFlow(User user) {
        return getDashboard(user, "all").cashFlow();
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
                                                        YearMonth selectedMonth) {
        return List.of(cashFlowPoint(transactions, selectedMonth));
    }

    private List<AnalyticsDtos.CashFlowPoint> cashFlow(List<FinancialTransaction> transactions) {
        Map<LocalDate, List<FinancialTransaction>> byMonth = new HashMap<>();
        transactions.forEach(transaction -> byMonth.computeIfAbsent(
                transaction.getTransactionDate().withDayOfMonth(1), ignored -> new ArrayList<>()).add(transaction));
        List<AnalyticsDtos.CashFlowPoint> points = new ArrayList<>();
        if (byMonth.isEmpty()) return points;
        LocalDate start = byMonth.keySet().stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate end = byMonth.keySet().stream().max(LocalDate::compareTo).orElseThrow();
        for (LocalDate month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            List<FinancialTransaction> monthTransactions = byMonth.getOrDefault(month, List.of());
            points.add(cashFlowPoint(monthTransactions, YearMonth.from(month)));
        }
        return points;
    }

    private AnalyticsDtos.CashFlowPoint cashFlowPoint(List<FinancialTransaction> transactions,
                                                        YearMonth month) {
        return new AnalyticsDtos.CashFlowPoint(
                month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        + " " + month.getYear(),
                sumCredits(transactions), sumDebits(transactions));
    }

    private List<AnalyticsDtos.MonthOption> availableMonths(List<FinancialTransaction> transactions) {
        return transactions.stream()
                .map(transaction -> YearMonth.from(transaction.getTransactionDate()))
                .distinct()
                .sorted(Comparator.reverseOrder())
                .map(month -> new AnalyticsDtos.MonthOption(month.toString(), monthLabel(month)))
                .toList();
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (java.time.DateTimeException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "month must use YYYY-MM format or be all");
        }
    }

    private String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + month.getYear();
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
                                                 BigDecimal spending, int budgetPeriods) {
        List<AnalyticsDtos.Insight> insights = new ArrayList<>();
        categories.stream().filter(category ->
                        category.amount().compareTo(category.budget().multiply(BigDecimal.valueOf(budgetPeriods))) > 0)
                .findFirst().ifPresent(category -> insights.add(new AnalyticsDtos.Insight(
                        "WARNING", category.name() + " is over budget",
                        category.name() + " spending is "
                                + category.amount().subtract(category.budget().multiply(
                                        BigDecimal.valueOf(budgetPeriods)))
                                .setScale(2, RoundingMode.HALF_UP)
                                + " over the budget for this period.")));
        if (savingsRate >= 20) {
            insights.add(new AnalyticsDtos.Insight("POSITIVE", "Strong savings rate",
                    "You saved " + String.format(Locale.ROOT, "%.1f", savingsRate)
                            + "% of income this period."));
        } else if (spending.signum() > 0) {
            insights.add(new AnalyticsDtos.Insight("INFO", "Review your spending",
                    "Review category spending to find opportunities to save."));
        }
        if (insights.size() < 3 && !categories.isEmpty()) {
            AnalyticsDtos.CategorySpend highest = categories.get(0);
            insights.add(new AnalyticsDtos.Insight("INFO", "Largest spending category",
                    highest.name() + " is your largest spending category this period."));
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
