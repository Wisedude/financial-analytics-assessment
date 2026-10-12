package com.example.financialanalytics.repository;

import com.example.financialanalytics.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<FinancialTransaction, UUID>,
        JpaSpecificationExecutor<FinancialTransaction> {
    Optional<FinancialTransaction> findByIdAndAccountId(UUID id, UUID accountId);

    List<FinancialTransaction> findByAccountIdAndTransactionDateBetweenOrderByTransactionDateDesc(
            UUID accountId, LocalDate from, LocalDate to);

    List<FinancialTransaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

    @Query("select coalesce(sum(tx.amount), 0) from FinancialTransaction tx "
            + "where tx.accountId = :accountId")
    java.math.BigDecimal sumAmountByAccountId(@Param("accountId") UUID accountId);

}
