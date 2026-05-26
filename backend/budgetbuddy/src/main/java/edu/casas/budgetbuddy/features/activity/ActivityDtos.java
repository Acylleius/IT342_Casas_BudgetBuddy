package edu.casas.budgetbuddy.features.activity;

import java.time.LocalDateTime;

public final class ActivityDtos {
    private ActivityDtos() {
    }

    public record ActivityDto(Long id, Long userId, String action, String entityType,
                              Long entityId, String description, LocalDateTime createdAt) {
    }
}
