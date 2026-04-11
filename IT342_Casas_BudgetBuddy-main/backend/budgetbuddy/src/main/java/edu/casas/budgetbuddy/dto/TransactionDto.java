package edu.casas.budgetbuddy.dto;

import edu.casas.budgetbuddy.entity.TransactionEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDto {

    private final Long id;
    private final Long userId;
    private final String type;
    private final BigDecimal amount;
    private final String category;
    private final String description;
    private final LocalDate transactionDate;
    private final LocalDateTime createdAt;

    public TransactionDto(
            Long id,
            Long userId,
            String type,
            BigDecimal amount,
            String category,
            String description,
            LocalDate transactionDate,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
    }

    public static TransactionDto from(TransactionEntry entry) {
        return new TransactionDto(
                entry.getId(),
                entry.getUser().getId(),
                entry.getType().name(),
                entry.getAmount(),
                entry.getCategory(),
                entry.getDescription(),
                entry.getTransactionDate(),
                entry.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
