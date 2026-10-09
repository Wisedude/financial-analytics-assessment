package com.example.financialanalytics.repository;

import com.example.financialanalytics.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByAccountIdOrderByCategory(UUID accountId);
    Optional<Budget> findByAccountIdAndCategoryIgnoreCase(UUID accountId, String category);
}
