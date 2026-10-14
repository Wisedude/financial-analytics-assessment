package com.example.financialanalytics.controller;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.service.AuthService;
import com.example.financialanalytics.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private static final Set<String> SORTABLE_FIELDS = Set.of("transactionDate", "amount", "merchant", "category");

    private final AuthService authService;
    private final TransactionService transactionService;

    public TransactionController(AuthService authService, TransactionService transactionService) {
        this.authService = authService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public AnalyticsDtos.PageResponse<AnalyticsDtos.TransactionResponse> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 25, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page size must not exceed 100");
        }
        if (pageable.getSort().stream().anyMatch(order -> !SORTABLE_FIELDS.contains(order.getProperty()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported transaction sort field");
        }
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toUpperCase();
        if (normalizedType != null && !Set.of("CREDIT", "DEBIT").contains(normalizedType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "type must be CREDIT or DEBIT");
        }
        Page<AnalyticsDtos.TransactionResponse> page = transactionService.search(
                authService.requireUser(authorization), category, normalizedType, from, to, pageable);
        return new AnalyticsDtos.PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @GetMapping("/{transactionId}")
    public AnalyticsDtos.TransactionResponse get(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID transactionId) {
        return transactionService.get(authService.requireUser(authorization), transactionId);
    }

    @PostMapping
    public ResponseEntity<AnalyticsDtos.TransactionResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @jakarta.validation.Valid @RequestBody AnalyticsDtos.CreateTransactionRequest request) {
        AnalyticsDtos.TransactionResponse transaction = transactionService.create(
                authService.requireUser(authorization), request);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + transaction.id()))
                .body(transaction);
    }

    @PutMapping("/{transactionId}")
    public AnalyticsDtos.TransactionResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID transactionId,
            @jakarta.validation.Valid @RequestBody AnalyticsDtos.CreateTransactionRequest request) {
        return transactionService.update(authService.requireUser(authorization), transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable UUID transactionId) {
        transactionService.delete(authService.requireUser(authorization), transactionId);
        return ResponseEntity.noContent().build();
    }
}
