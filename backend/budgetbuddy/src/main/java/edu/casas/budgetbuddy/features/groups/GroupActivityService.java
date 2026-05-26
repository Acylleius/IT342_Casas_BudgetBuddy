package edu.casas.budgetbuddy.features.groups;

import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupActivityLogRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GroupActivityService {
    private final BudgetBuddyStore store;
    private final GroupsService groupsService;
    private final DatabasePersistenceService databasePersistenceService;

    public GroupActivityService(BudgetBuddyStore store, GroupsService groupsService,
                                DatabasePersistenceService databasePersistenceService) {
        this.store = store;
        this.groupsService = groupsService;
        this.databasePersistenceService = databasePersistenceService;
    }

    public synchronized GroupActivityDto log(Long groupId, Long actorUserId, String actionType,
                                             String entityType, Long entityId, String oldValue,
                                             String newValue, String description) {
        GroupActivityLogRecord record = new GroupActivityLogRecord(store.groupActivityIds.getAndIncrement(),
                groupId, actorUserId, displayName(actorUserId), actionType, entityType, entityId,
                oldValue, newValue, description, LocalDateTime.now());
        store.groupActivityLogs.add(record);
        databasePersistenceService.saveGroupActivity(record);
        return toDto(record);
    }

    public List<GroupActivityDto> history(Long requesterId, Long groupId) {
        if (!groupsService.isMember(groupId, requesterId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Group membership required");
        }
        return store.groupActivityLogs.stream()
                .filter(activity -> activity.groupId().equals(groupId))
                .sorted(Comparator.comparing(GroupActivityLogRecord::createdAt).reversed())
                .map(this::toDto)
                .toList();
    }

    public String displayName(Long userId) {
        return store.users.stream()
                .filter(user -> user.id().equals(userId))
                .findFirst()
                .map(this::displayName)
                .orElse("User #" + userId);
    }

    private String displayName(UserRecord user) {
        String fullName = (user.firstname() + " " + user.lastname()).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        int at = user.email().indexOf('@');
        return at > 0 ? user.email().substring(0, at) : user.email();
    }

    private GroupActivityDto toDto(GroupActivityLogRecord record) {
        return new GroupActivityDto(record.id(), record.groupId(), record.actorUserId(), record.actorUsername(),
                record.actionType(), record.entityType(), record.entityId(), record.oldValue(), record.newValue(),
                record.description(), record.createdAt());
    }

    public record GroupActivityDto(Long id, Long groupId, Long actorUserId, String actorUsername,
                                   String actionType, String entityType, Long entityId,
                                   String oldValue, String newValue, String description,
                                   LocalDateTime createdAt) {
    }
}
