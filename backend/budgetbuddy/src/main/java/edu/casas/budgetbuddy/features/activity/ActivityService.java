package edu.casas.budgetbuddy.features.activity;

import edu.casas.budgetbuddy.features.activity.ActivityDtos.ActivityDto;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.ActivityLogRecord;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
    private final BudgetBuddyStore store;

    public ActivityService(BudgetBuddyStore store) {
        this.store = store;
    }

    public synchronized ActivityDto log(Long userId, String action, String entityType,
                                        Long entityId, String description) {
        ActivityLogRecord record = new ActivityLogRecord(store.activityIds.getAndIncrement(), userId,
                action, entityType, entityId, description, LocalDateTime.now());
        store.activityLogs.add(record);
        return toDto(record);
    }

    public List<ActivityDto> recent(int limit) {
        return store.activityLogs.stream()
                .sorted(Comparator.comparing(ActivityLogRecord::createdAt).reversed())
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(this::toDto)
                .toList();
    }

    public List<ActivityDto> recentForUser(Long userId, int limit) {
        return store.activityLogs.stream()
                .filter(activity -> activity.userId().equals(userId))
                .sorted(Comparator.comparing(ActivityLogRecord::createdAt).reversed())
                .limit(Math.max(1, Math.min(limit, 100)))
                .map(this::toDto)
                .toList();
    }

    private ActivityDto toDto(ActivityLogRecord record) {
        return new ActivityDto(record.id(), record.userId(), record.action(), record.entityType(),
                record.entityId(), record.description(), record.createdAt());
    }
}
