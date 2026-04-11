package edu.casas.budgetbuddy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionRequest {

    @NotNull(message = "User ID is required.")
    private Long userId;

    @NotBlank(message = "Transaction type is required.")
    private String type;

    @NotNull(message = "Amount is required.")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero.")
    private BigDecimal amount;

    @NotBlank(message = "Category is required.")
    @Size(max = 80, message = "Category must not exceed 80 characters.")
    private String category;

    @Size(max = 200, message = "Description must not exceed 200 characters.")
    private String description;

    @NotNull(message = "Transaction date is required.")
    private LocalDate transactionDate;

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
}
