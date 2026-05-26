package edu.casas.budgetbuddy.features.grouptransactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class GroupTransactionsDtos {
    private GroupTransactionsDtos() {
    }

    public record GroupTransactionRequest(@NotBlank String type,
                                          @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                          @NotBlank String category,
                                          String description,
                                          Long actorUserId,
                                          LocalDate transactionDate) {
    }

    public record GroupTransactionDto(Long id, Long groupId, Long actorUserId, String actorUsername,
                                      String type, BigDecimal amount, String category, String description,
                                      LocalDate transactionDate, LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {
    }

    public record GroupSummaryDto(BigDecimal totalIncome, BigDecimal totalExpenses, BigDecimal netBalance) {
    }
}
