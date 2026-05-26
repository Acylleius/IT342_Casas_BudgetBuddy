package edu.casas.budgetbuddy.features.inbox;

import edu.casas.budgetbuddy.features.realtime.RealtimeService;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.InboxNotificationRecord;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InboxService {
    private final BudgetBuddyStore store;
    private final RealtimeService realtimeService;
    private final DatabasePersistenceService databasePersistenceService;

    public InboxService(BudgetBuddyStore store, RealtimeService realtimeService,
                        DatabasePersistenceService databasePersistenceService) {
        this.store = store;
        this.realtimeService = realtimeService;
        this.databasePersistenceService = databasePersistenceService;
    }

    public synchronized InboxNotificationDto welcome(Long userId, String firstname) {
        return create(userId, null, null, "WELCOME", "Welcome to BudgetBuddy",
                "Welcome to BudgetBuddy, " + firstname + "! Start by creating a group, adding expenses, or tracking your personal transactions.");
    }

    public synchronized InboxNotificationDto create(Long recipientUserId, Long groupId, Long invitationId,
                                                    String type, String title, String message) {
        InboxNotificationRecord record = new InboxNotificationRecord(store.inboxIds.getAndIncrement(),
                recipientUserId, groupId, invitationId, type, title, message, false, LocalDateTime.now());
        store.inboxNotifications.add(record);
        databasePersistenceService.saveInbox(record);
        InboxNotificationDto dto = toDto(record);
        realtimeService.publishToUser(recipientUserId, "inbox-updated", dto);
        return dto;
    }

    public List<InboxNotificationDto> list(Long userId) {
        return store.inboxNotifications.stream()
                .filter(notification -> notification.recipientUserId().equals(userId))
                .sorted(Comparator.comparing(InboxNotificationRecord::createdAt).reversed())
                .map(this::toDto)
                .toList();
    }

    public synchronized void markRead(Long userId, Long id) {
        for (int index = 0; index < store.inboxNotifications.size(); index++) {
            InboxNotificationRecord current = store.inboxNotifications.get(index);
            if (current.id().equals(id)) {
                if (!current.recipientUserId().equals(userId)) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "Cannot update another user's inbox item");
                }
                InboxNotificationRecord replacement = new InboxNotificationRecord(current.id(),
                        current.recipientUserId(), current.groupId(), current.invitationId(),
                        current.type(), current.title(), current.message(), true, current.createdAt());
                store.inboxNotifications.set(index, replacement);
                databasePersistenceService.saveInbox(replacement);
                realtimeService.publishToUser(userId, "inbox-updated", unreadCount(userId));
                return;
            }
        }
        throw new DomainException(HttpStatus.NOT_FOUND, "Inbox item not found");
    }

    public synchronized void markInvitationRead(Long userId, Long invitationId) {
        for (int index = 0; index < store.inboxNotifications.size(); index++) {
            InboxNotificationRecord current = store.inboxNotifications.get(index);
            if (current.recipientUserId().equals(userId) && invitationId.equals(current.invitationId())) {
                InboxNotificationRecord replacement = new InboxNotificationRecord(current.id(),
                        current.recipientUserId(), current.groupId(), current.invitationId(),
                        current.type(), current.title(), current.message(), true, current.createdAt());
                store.inboxNotifications.set(index, replacement);
                databasePersistenceService.saveInbox(replacement);
            }
        }
        realtimeService.publishToUser(userId, "inbox-updated", unreadCount(userId));
    }

    public synchronized void markAllRead(Long userId) {
        for (int index = 0; index < store.inboxNotifications.size(); index++) {
            InboxNotificationRecord current = store.inboxNotifications.get(index);
            if (current.recipientUserId().equals(userId) && !current.read()) {
                InboxNotificationRecord replacement = new InboxNotificationRecord(current.id(),
                        current.recipientUserId(), current.groupId(), current.invitationId(),
                        current.type(), current.title(), current.message(), true, current.createdAt());
                store.inboxNotifications.set(index, replacement);
                databasePersistenceService.saveInbox(replacement);
            }
        }
        realtimeService.publishToUser(userId, "inbox-updated", unreadCount(userId));
    }

    public long unreadCount(Long userId) {
        return store.inboxNotifications.stream()
                .filter(notification -> notification.recipientUserId().equals(userId) && !notification.read())
                .count();
    }

    private InboxNotificationDto toDto(InboxNotificationRecord record) {
        return new InboxNotificationDto(record.id(), record.recipientUserId(), record.groupId(),
                record.invitationId(), invitationStatus(record.invitationId()), record.type(),
                record.title(), record.message(), record.read(), unreadCount(record.recipientUserId()),
                record.createdAt());
    }

    public record InboxNotificationDto(Long id, Long recipientUserId, Long groupId, Long invitationId,
                                       String invitationStatus, String type, String title, String message,
                                       boolean isRead, long unreadCount, LocalDateTime createdAt) {
    }

    private String invitationStatus(Long invitationId) {
        if (invitationId == null) {
            return null;
        }
        return store.groupInvitations.stream()
                .filter(invitation -> invitation.id().equals(invitationId))
                .map(BudgetBuddyStore.GroupInvitationRecord::status)
                .findFirst()
                .orElse(null);
    }
}
