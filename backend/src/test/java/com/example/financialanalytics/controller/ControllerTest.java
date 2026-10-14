package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.dto.AuthDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.service.AnalyticsService;
import com.example.financialanalytics.service.AuthService;
import com.example.financialanalytics.service.BudgetService;
import com.example.financialanalytics.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerTest {
    private final AuthService authService = mock(AuthService.class);
    private final AnalyticsService analyticsService = mock(AnalyticsService.class);
    private final BudgetService budgetService = mock(BudgetService.class);
    private final TransactionService transactionService = mock(TransactionService.class);
    private final User user = new User(UUID.randomUUID(), "Test User", "test@example.com", "hash");
    private final AuthDtos.AuthResponse authResponse = new AuthDtos.AuthResponse(
            "token", new AuthDtos.UserResponse(user.getId().toString(), user.getName(), user.getEmail()));
    private final AnalyticsDtos.TransactionResponse transaction = new AnalyticsDtos.TransactionResponse(
            UUID.randomUUID().toString(), LocalDate.parse("2026-10-01"), "Market", "Groceries",
            new BigDecimal("-10.00"), "DEBIT");

    @BeforeEach
    void setUp() {
        when(authService.requireUser("Bearer token")).thenReturn(user);
    }

    @Test
    void authControllerReturnsNoStoreResponsesAndRevokesSessions() {
        AuthController controller = new AuthController(authService);
        when(authService.login(any())).thenReturn(authResponse);
        when(authService.register(any())).thenReturn(authResponse);
        when(authService.loginWithGoogle("credential")).thenReturn(authResponse);

        assertEquals(HttpStatus.OK, controller.login(new AuthDtos.LoginRequest("test@example.com", "password"))
                .getStatusCode());
        var registration = controller.register(new AuthDtos.RegisterRequest("Test User",
                "test@example.com", "password123"));
        assertEquals(HttpStatus.CREATED, registration.getStatusCode());
        assertEquals("/api/v1/account", registration.getHeaders().getLocation().getPath());
        assertEquals(HttpStatus.OK, controller.googleLogin(new AuthDtos.GoogleLoginRequest("credential"))
                .getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT,
                controller.linkGoogle("Bearer token", new AuthDtos.GoogleLoginRequest("credential")).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.logout("Bearer token").getStatusCode());
        verify(authService).linkGoogleIdentity(user.getId(), "credential");
    }

    @Test
    void analyticsControllerHandlesDefaultAndExplicitMonth() {
        AnalyticsController controller = new AnalyticsController(authService, analyticsService);
        var expected = emptyDashboard();
        when(analyticsService.getDashboard(user)).thenReturn(expected);
        when(analyticsService.getDashboard(user, "2026-10")).thenReturn(expected);

        assertEquals(expected, controller.dashboard("Bearer token", null));
        assertEquals(expected, controller.dashboard("Bearer token", "2026-10"));
        controller.cashFlow("Bearer token");
        controller.spending("Bearer token");
        verify(analyticsService).getCashFlow(user);
        verify(analyticsService).getSpending(user);
    }

    @Test
    void accountControllerReturnsUserAndAccountDetails() {
        when(analyticsService.getAccount(user)).thenReturn(
                new AnalyticsDtos.AccountResponse("account-id", "ZAR", new BigDecimal("10.00")));

        var result = new AccountController(authService, analyticsService).get("Bearer token");

        assertEquals(user.getEmail(), result.user().email());
        assertEquals("account-id", result.account().id());
    }

    @Test
    void budgetControllerListsAndUpdatesBudget() {
        var budget = new AnalyticsDtos.BudgetResponse("Groceries",
                new BigDecimal("10.00"), new BigDecimal("100.00"), 10.0);
        when(budgetService.getBudgets(user)).thenReturn(List.of(budget));
        when(budgetService.updateBudget(user, "Groceries", new BigDecimal("100.00"))).thenReturn(budget);
        BudgetController controller = new BudgetController(authService, budgetService);

        assertEquals(List.of(budget), controller.list("Bearer token"));
        assertEquals(budget, controller.update("Bearer token", "Groceries",
                new AnalyticsDtos.UpdateBudgetRequest(new BigDecimal("100.00"))));
    }

    @Test
    void transactionControllerListsAndValidatesFilters() {
        PageRequest pageable = PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "transactionDate"));
        when(transactionService.search(eq(user), eq("Groceries"), eq("DEBIT"),
                eq(LocalDate.parse("2026-10-01")), eq(LocalDate.parse("2026-10-31")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        TransactionController controller = new TransactionController(authService, transactionService);

        var page = controller.list("Bearer token", "Groceries", "debit",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), pageable);

        assertEquals(1, page.totalElements());
        assertEquals(transaction, page.content().get(0));
        assertThrows(ApiException.class, () -> controller.list("Bearer token", null, null,
                null, null, PageRequest.of(0, 101)));
        assertThrows(ApiException.class, () -> controller.list("Bearer token", null, null, null, null,
                PageRequest.of(0, 10, Sort.by("password"))));
        assertThrows(ApiException.class, () -> controller.list("Bearer token", null, "wire",
                null, null, PageRequest.of(0, 10)));
        controller.get("Bearer token", UUID.fromString(transaction.id()));
        when(transactionService.create(eq(user), any())).thenReturn(transaction);
        var created = controller.create("Bearer token", new AnalyticsDtos.CreateTransactionRequest(
                LocalDate.parse("2026-10-01"), "Market", "Groceries",
                new BigDecimal("10.00"), "DEBIT"));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertEquals("/api/v1/transactions/" + transaction.id(), created.getHeaders().getLocation().getPath());

        when(transactionService.update(eq(user), eq(UUID.fromString(transaction.id())), any()))
                .thenReturn(transaction);
        assertEquals(transaction, controller.update("Bearer token", UUID.fromString(transaction.id()),
                new AnalyticsDtos.CreateTransactionRequest(LocalDate.parse("2026-10-01"),
                        "Market", "Groceries", new BigDecimal("10.00"), "DEBIT")));
        assertEquals(HttpStatus.NO_CONTENT,
                controller.delete("Bearer token", UUID.fromString(transaction.id())).getStatusCode());
        verify(transactionService).delete(user, UUID.fromString(transaction.id()));
    }

    @Test
    void transactionControllerAllowsBlankTypeAndNormalizesValidType() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(transactionService.search(eq(user), eq(" Groceries "), eq("CREDIT"),
                eq(null), eq(null), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        new TransactionController(authService, transactionService).list(
                "Bearer token", " Groceries ", " credit ", null, null, pageable);

        when(transactionService.search(eq(user), eq(null), eq(null),
                eq(null), eq(null), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));
        new TransactionController(authService, transactionService).list(
                "Bearer token", null, " ", null, null, pageable);
    }

    private AnalyticsDtos.DashboardResponse emptyDashboard() {
        var summary = new AnalyticsDtos.Summary("ZAR", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 0, BigDecimal.ZERO, "All time");
        return new AnalyticsDtos.DashboardResponse(summary, List.of(), List.of(), List.of(),
                List.of(), List.of(), "all");
    }
}
