package edu.casas.budgetbuddy.features.budgets;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BudgetDtos {
    private BudgetDtos() {
    }

    public record BudgetRequest(@NotBlank String name,
                                @NotNull @DecimalMin(value = "0.01") BigDecimal limitAmount,
                                @NotBlank String period,
                                String category,
                                LocalDate startDate,
                                LocalDate endDate) {
    }

    public record BudgetDto(Long id, String scope, Long userId, Long groupId, Long createdByUserId,
                            String name, BigDecimal limitAmount, String period, String category,
                            LocalDate startDate, LocalDate endDate, BigDecimal spentAmount,
                            BigDecimal remainingAmount, BigDecimal percentageUsed, String status,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record BudgetTrackingDto(BudgetDto budget, List<RelatedTransactionDto> relatedTransactions,
                                    List<SpenderDto> contributors, LocalDateTime lastUpdated) {
    }

    public record RelatedTransactionDto(Long id, Long userId, Long groupId, String type,
                                        BigDecimal amount, String category, String description,
                                        LocalDate transactionDate) {
    }

    public record SpenderDto(Long userId, String displayName, BigDecimal amount) {
    }
}
