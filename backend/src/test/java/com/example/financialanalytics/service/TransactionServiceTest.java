package com.example.financialanalytics.service;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.Account;
import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final AnalyticsService analyticsService = mock(AnalyticsService.class);
    private final TransactionService service = new TransactionService(
            accountRepository, transactionRepository, analyticsService);
    private final User user = new User(userId, "Test User", "test@example.com", "password-hash");
    private final FinancialTransaction transaction = new FinancialTransaction(transactionId, accountId,
            LocalDate.parse("2026-10-12"), "Market", "Groceries",
            new BigDecimal("-25.00"), "DEBIT");
    private final AnalyticsDtos.TransactionResponse response = new AnalyticsDtos.TransactionResponse(
            transactionId.toString(), transaction.getTransactionDate(), transaction.getMerchant(),
            transaction.getCategory(), transaction.getAmount(), transaction.getType());

    @BeforeEach
    void setUp() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(
                new Account(accountId, userId, "ZAR", new BigDecimal("500.00"))));
        when(analyticsService.toResponse(transaction)).thenReturn(response);
    }

    @Test
    void searchesOwnedTransactionsWithOptionalFilters() {
        var pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "transactionDate"));
        when(transactionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable))).thenReturn(new PageImpl<>(List.of(transaction)));

        var results = service.search(user, " groceries ", "DEBIT",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), pageable);

        assertEquals(1, results.getTotalElements());
        assertEquals(response, results.getContent().get(0));
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThrows(ApiException.class, () -> service.search(user, null, null,
                LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-01"), PageRequest.of(0, 10)));
    }

    @Test
    void returnsNotFoundWhenSearchingWithoutAnAccount() {
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> service.search(user, null, null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    void retrievesAnOwnedTransaction() {
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        assertEquals(response, service.get(user, transactionId));
    }

    @Test
    void hidesTransactionsThatDoNotBelongToTheAccount() {
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class, () -> service.get(user, transactionId));
    }

    @Test
    void storesDebitAmountsAsNegativeAndTrimsTextFields() {
        when(transactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(analyticsService.toResponse(any(FinancialTransaction.class))).thenAnswer(invocation -> {
            FinancialTransaction saved = invocation.getArgument(0);
            return new AnalyticsDtos.TransactionResponse(saved.getId().toString(),
                    saved.getTransactionDate(), saved.getMerchant(), saved.getCategory(),
                    saved.getAmount(), saved.getType());
        });
        var request = new AnalyticsDtos.CreateTransactionRequest(LocalDate.parse("2026-10-12"),
                " Market ", " Groceries ", new BigDecimal("12.30"), "debit");

        var result = service.create(user, request);

        assertEquals("-12.30", result.amount().toPlainString());
        assertEquals("Market", result.merchant());
        assertEquals("Groceries", result.category());
        assertEquals("DEBIT", result.type());
        assertNotNull(result.id());
        verify(transactionRepository).save(any(FinancialTransaction.class));
    }

    @Test
    void storesCreditsAsPositive() {
        when(transactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(analyticsService.toResponse(any(FinancialTransaction.class))).thenAnswer(invocation -> {
            FinancialTransaction saved = invocation.getArgument(0);
            return new AnalyticsDtos.TransactionResponse(saved.getId().toString(),
                    saved.getTransactionDate(), saved.getMerchant(), saved.getCategory(),
                    saved.getAmount(), saved.getType());
        });
        var request = new AnalyticsDtos.CreateTransactionRequest(LocalDate.parse("2026-10-12"),
                "Employer", "Income", new BigDecimal("200.00"), "CREDIT");

        assertEquals("200.00", service.create(user, request).amount().toPlainString());
    }

    @Test
    void updatesOwnedTransactionAndAppliesDebitSign() {
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(analyticsService.toResponse(transaction)).thenReturn(response);
        var request = new AnalyticsDtos.CreateTransactionRequest(LocalDate.parse("2026-10-14"),
                " New Market ", " Food ", new BigDecimal("40.50"), "DEBIT");

        service.update(user, transactionId, request);

        assertEquals(LocalDate.parse("2026-10-14"), transaction.getTransactionDate());
        assertEquals("New Market", transaction.getMerchant());
        assertEquals("Food", transaction.getCategory());
        assertEquals("-40.50", transaction.getAmount().toPlainString());
        assertEquals("DEBIT", transaction.getType());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void updatesToCreditAsPositive() {
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(analyticsService.toResponse(transaction)).thenReturn(response);

        service.update(user, transactionId, new AnalyticsDtos.CreateTransactionRequest(
                LocalDate.parse("2026-10-14"), "Employer", "Income",
                new BigDecimal("800.00"), "CREDIT"));

        assertEquals("800.00", transaction.getAmount().toPlainString());
        assertEquals("CREDIT", transaction.getType());
    }

    @Test
    void doesNotUpdateOrDeleteTransactionsOutsideTheUsersAccount() {
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.empty());
        var updateRequest = new AnalyticsDtos.CreateTransactionRequest(
                LocalDate.parse("2026-10-14"), "Market", "Food", BigDecimal.TEN, "DEBIT");

        assertThrows(ApiException.class, () -> service.update(user, transactionId, updateRequest));
        assertThrows(ApiException.class, () -> service.delete(user, transactionId));
        verify(transactionRepository, never()).save(any(FinancialTransaction.class));
        verify(transactionRepository, never()).delete(any(FinancialTransaction.class));
    }

    @Test
    void deletesOwnedTransaction() {
        when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
                .thenReturn(Optional.of(transaction));

        service.delete(user, transactionId);

        verify(transactionRepository).delete(transaction);
    }
}
