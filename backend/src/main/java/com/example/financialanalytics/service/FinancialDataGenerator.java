package com.example.financialanalytics.service;

import com.example.financialanalytics.model.Account;
import com.example.financialanalytics.model.Budget;
import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.BudgetRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class FinancialDataGenerator {
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("15000.00");
    private static final List<CategoryTemplate> CATEGORIES = List.of(
            new CategoryTemplate("Housing", "9500.00", "Rent Payment", 9300, 0.01),
            new CategoryTemplate("Groceries", "5600.00", "Grocery Market", 4700, 0.16),
            new CategoryTemplate("Transport", "4200.00", "Fuel Station", 3100, 0.20),
            new CategoryTemplate("Dining", "3000.00", "Restaurant", 2250, 0.28),
            new CategoryTemplate("Shopping", "3200.00", "Retail Store", 1950, 0.32),
            new CategoryTemplate("Utilities", "2300.00", "Utilities Provider", 1850, 0.08),
            new CategoryTemplate("Subscriptions", "1300.00", "Streaming Service", 850, 0.04),
            new CategoryTemplate("Health", "1800.00", "Pharmacy", 650, 0.25),
            new CategoryTemplate("Other", "2000.00", "Local Merchant", 1050, 0.35));

    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public FinancialDataGenerator(AccountRepository accountRepository,
                                  BudgetRepository budgetRepository,
                                  TransactionRepository transactionRepository,
                                  Clock clock) {
        this.accountRepository = accountRepository;
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public Account createAccountWithSampleData(UUID userId) {
        Account account = new Account(UUID.randomUUID(), userId, "ZAR", OPENING_BALANCE);
        accountRepository.saveAndFlush(account);
        budgetRepository.saveAll(CATEGORIES.stream()
                .map(category -> new Budget(UUID.randomUUID(), account.getId(), category.name(),
                        new BigDecimal(category.budget()).setScale(2, RoundingMode.HALF_UP)))
                .toList());
        transactionRepository.saveAll(createTransactions(account.getId(), userId));
        return account;
    }

    List<FinancialTransaction> createTransactions(UUID accountId, UUID userId) {
        LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
        Random random = new Random(userId.getMostSignificantBits() ^ userId.getLeastSignificantBits());
        var transactions = new java.util.ArrayList<FinancialTransaction>();
        for (int monthOffset = 5; monthOffset >= 0; monthOffset--) {
            LocalDate month = currentMonth.minusMonths(monthOffset);
            BigDecimal salary = amount(41000 + random.nextInt(5001));
            BigDecimal supplementaryIncome = amount(700 + random.nextInt(1001));
            transactions.add(transaction(accountId, transactionDate(month, 25), "Monthly Salary",
                    "Income", salary, "CREDIT"));
            transactions.add(transaction(accountId, transactionDate(month, 15), "Freelance Payment",
                    "Income", supplementaryIncome, "CREDIT"));

            int categoryIndex = 0;
            for (CategoryTemplate category : CATEGORIES) {
                double variation = category.monthlySpend() * (random.nextDouble() * 2 - 1)
                        * category.variation();
                BigDecimal spend = amount(Math.max(10,
                        Math.round(category.monthlySpend() + variation)));
                int day = Math.min(28, 3 + categoryIndex * 3);
                transactions.add(transaction(accountId, transactionDate(month, day),
                        category.merchant(), category.name(), spend.negate(), "DEBIT"));
                categoryIndex++;
            }
        }
        return transactions;
    }

    private FinancialTransaction transaction(UUID accountId, LocalDate date, String merchant,
                                               String category, BigDecimal amount, String type) {
        return new FinancialTransaction(UUID.randomUUID(), accountId, date, merchant, category, amount, type);
    }

    private BigDecimal amount(long amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate transactionDate(LocalDate month, int day) {
        LocalDate date = month.withDayOfMonth(day);
        LocalDate today = LocalDate.now(clock);
        return date.isAfter(today) ? today : date;
    }

    private record CategoryTemplate(String name, String budget, String merchant,
                                    double monthlySpend, double variation) {}
}
