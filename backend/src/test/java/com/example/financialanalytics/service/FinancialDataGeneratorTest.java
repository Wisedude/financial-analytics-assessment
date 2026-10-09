package com.example.financialanalytics.service;

import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.BudgetRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FinancialDataGeneratorTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
    private final FinancialDataGenerator generator = new FinancialDataGenerator(
            mock(AccountRepository.class), mock(BudgetRepository.class),
            mock(TransactionRepository.class), clock);

    @Test
    void createsSixMonthsOfRealisticBalancedTransactions() {
        UUID userId = UUID.fromString("7b339a82-43f4-42a3-90c1-6f076183c42f");
        List<FinancialTransaction> transactions = generator.createTransactions(UUID.randomUUID(), userId);

        assertEquals(66, transactions.size());
        assertFalse(transactions.stream().anyMatch(transaction ->
                transaction.getTransactionDate().isAfter(java.time.LocalDate.now(clock))));
        assertTrue(transactions.stream().allMatch(transaction ->
                ("CREDIT".equals(transaction.getType()) && transaction.getAmount().signum() > 0)
                        || ("DEBIT".equals(transaction.getType()) && transaction.getAmount().signum() < 0)));

        BigDecimal income = transactions.stream().filter(transaction -> "CREDIT".equals(transaction.getType()))
                .map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal spending = transactions.stream().filter(transaction -> "DEBIT".equals(transaction.getType()))
                .map(FinancialTransaction::getAmount).map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(income.compareTo(new BigDecimal("250000.00")) > 0);
        assertTrue(spending.compareTo(new BigDecimal("130000.00")) > 0);
        assertTrue(income.compareTo(spending) > 0, "monthly income should exceed spending");

        BigDecimal secondRunIncome = generator.createTransactions(UUID.randomUUID(), userId).stream()
                .filter(transaction -> "CREDIT".equals(transaction.getType()))
                .map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(income, secondRunIncome, "generated financial values should be repeatable per user");
    }
}
