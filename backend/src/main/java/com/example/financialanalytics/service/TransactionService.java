package com.example.financialanalytics.service;

import com.example.financialanalytics.dto.AnalyticsDtos;
import com.example.financialanalytics.exception.ApiException;
import com.example.financialanalytics.model.Account;
import com.example.financialanalytics.model.FinancialTransaction;
import com.example.financialanalytics.model.User;
import com.example.financialanalytics.repository.AccountRepository;
import com.example.financialanalytics.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AnalyticsService analyticsService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              AnalyticsService analyticsService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.analyticsService = analyticsService;
    }

    @Transactional(readOnly = true)
    public Page<AnalyticsDtos.TransactionResponse> search(
            User user, String category, String type, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from must be on or before to");
        }
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        Specification<FinancialTransaction> specification = (root, query, criteria) ->
                criteria.equal(root.get("accountId"), account.getId());
        if (category != null && !category.isBlank()) {
            String normalizedCategory = category.trim().toLowerCase(java.util.Locale.ROOT);
            specification = specification.and((root, query, criteria) ->
                    criteria.equal(criteria.lower(root.get("category")), normalizedCategory));
        }
        if (type != null) {
            specification = specification.and((root, query, criteria) ->
                    criteria.equal(root.get("type"), type));
        }
        if (from != null) {
            specification = specification.and((root, query, criteria) ->
                    criteria.greaterThanOrEqualTo(root.get("transactionDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, criteria) ->
                    criteria.lessThanOrEqualTo(root.get("transactionDate"), to));
        }
        return transactionRepository.findAll(specification, pageable).map(analyticsService::toResponse);
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.TransactionResponse get(User user, UUID transactionId) {
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        FinancialTransaction transaction = transactionRepository.findByIdAndAccountId(
                        transactionId, account.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        return analyticsService.toResponse(transaction);
    }

    @Transactional
    public AnalyticsDtos.TransactionResponse create(
            User user, AnalyticsDtos.CreateTransactionRequest request) {
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        String type = normalizeType(request.type());
        BigDecimal amount = signedAmount(request.amount(), type);
        FinancialTransaction transaction = new FinancialTransaction(UUID.randomUUID(), account.getId(),
                request.date(), request.merchant().trim(), request.category().trim(), amount, type);
        return analyticsService.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public AnalyticsDtos.TransactionResponse update(
            User user, UUID transactionId, AnalyticsDtos.CreateTransactionRequest request) {
        FinancialTransaction transaction = findOwnedTransaction(user, transactionId);
        String type = normalizeType(request.type());
        transaction.updateDetails(request.date(), request.merchant().trim(), request.category().trim(),
                signedAmount(request.amount(), type), type);
        return analyticsService.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(User user, UUID transactionId) {
        transactionRepository.delete(findOwnedTransaction(user, transactionId));
    }

    private FinancialTransaction findOwnedTransaction(User user, UUID transactionId) {
        Account account = accountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
        return transactionRepository.findByIdAndAccountId(transactionId, account.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private String normalizeType(String type) {
        return type.toUpperCase(java.util.Locale.ROOT);
    }

    private BigDecimal signedAmount(BigDecimal amount, String type) {
        BigDecimal normalizedAmount = amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
        return "DEBIT".equals(type) ? normalizedAmount.negate() : normalizedAmount;
    }
}
