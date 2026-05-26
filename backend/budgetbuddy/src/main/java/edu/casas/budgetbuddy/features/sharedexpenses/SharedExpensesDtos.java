package edu.casas.budgetbuddy.features.sharedexpenses;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SharedExpensesDtos {
    private SharedExpensesDtos() {
    }

    public record SharedExpenseRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                       @NotBlank String category,
                                       String description,
                                       Long groupId,
                                       @NotNull Long paidBy,
                                       LocalDate expenseDate,
                                       List<Long> participantUserIds) {
    }

    public record SharedExpenseDto(Long id, Long groupId, Long paidBy, BigDecimal amount,
                                   String category, String description, LocalDate expenseDate,
                                   List<SplitDto> splits) {
    }

    public record SplitDto(Long id, Long userId, BigDecimal amount, boolean settled) {
    }

    public record BalanceDto(Long userId, BigDecimal paid, BigDecimal owed, BigDecimal netBalance) {
    }

    public record BalanceExpense(BigDecimal amount, Long paidBy) {
    }

    public record BalanceMember(Long userId) {
    }
}
