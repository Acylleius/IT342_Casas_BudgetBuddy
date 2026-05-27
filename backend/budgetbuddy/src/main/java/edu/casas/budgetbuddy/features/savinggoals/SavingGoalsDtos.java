package edu.casas.budgetbuddy.features.savinggoals;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SavingGoalsDtos {
    private SavingGoalsDtos() {
    }

    public record SavingGoalRequest(@NotBlank String title,
                                    @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
                                    @NotNull @DecimalMin(value = "0.00") BigDecimal currentAmount,
                                    LocalDate deadline) {
    }

    public record ContributionRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                      String note) {
    }

    public record SavingGoalDto(Long id, String scope, Long userId, Long groupId, Long createdByUserId,
                                String title, BigDecimal targetAmount, BigDecimal currentAmount,
                                BigDecimal remainingAmount, BigDecimal percentageUsed,
                                LocalDate deadline, String status, LocalDateTime createdAt,
                                LocalDateTime updatedAt, List<ContributionDto> contributions) {
    }

    public record ContributionDto(Long id, Long userId, BigDecimal amount, String note,
                                  LocalDateTime createdAt) {
    }
}
