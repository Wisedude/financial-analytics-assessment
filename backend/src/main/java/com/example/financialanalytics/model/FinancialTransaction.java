package com.example.financialanalytics.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class FinancialTransaction {
    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, length = 120)
    private String merchant;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String type;

    protected FinancialTransaction() {}

    public FinancialTransaction(UUID id, UUID accountId, LocalDate transactionDate,
                                 String merchant, String category, BigDecimal amount, String type) {
        this.id = id;
        this.accountId = accountId;
        this.transactionDate = transactionDate;
        this.merchant = merchant;
        this.category = category;
        this.amount = amount;
        this.type = type;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public String getMerchant() { return merchant; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
}
