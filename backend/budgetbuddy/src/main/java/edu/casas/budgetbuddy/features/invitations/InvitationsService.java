package edu.casas.budgetbuddy.features.invitations;

import edu.casas.budgetbuddy.features.groups.GroupActivityService;
import edu.casas.budgetbuddy.features.groups.GroupsService;
import edu.casas.budgetbuddy.features.inbox.InboxService;
import edu.casas.budgetbuddy.features.invitations.InvitationsDtos.InvitationDto;
import edu.casas.budgetbuddy.features.notifications.NotificationService;
import edu.casas.budgetbuddy.features.realtime.RealtimeService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupInvitationRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupMemberRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class InvitationsService {
    private final BudgetBuddyStore store;
    private final GroupsService groupsService;
    private final GroupActivityService groupActivityService;
    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;
    private final DatabasePersistenceService databasePersistenceService;

    public InvitationsService(BudgetBuddyStore store, GroupsService groupsService,
                              GroupActivityService groupActivityService, InboxService inboxService,
                              NotificationService notificationService, RealtimeService realtimeService,
                              DatabasePersistenceService databasePersistenceService) {
        this.store = store;
        this.groupsService = groupsService;
        this.groupActivityService = groupActivityService;
        this.inboxService = inboxService;
        this.notificationService = notificationService;
        this.realtimeService = realtimeService;
        this.databasePersistenceService = databasePersistenceService;
    }

    public synchronized InvitationDto invite(Long requesterId, Long groupId, String email) {
        groupsService.requireMember(groupId, requesterId);
        UserRecord invited = findUserByEmail(email);
        if (groupsService.isMember(groupId, invited.id())) {
            throw new DomainException(HttpStatus.CONFLICT, "User is already a member");
        }
        if (hasPendingInvitation(groupId, invited.id())) {
            throw new DomainException(HttpStatus.CONFLICT, "User already invited");
        }
        GroupInvitationRecord invitation = new GroupInvitationRecord(store.invitationIds.getAndIncrement(),
                groupId, invited.id(), requesterId, "PENDING", LocalDateTime.now(), null);
        store.groupInvitations.add(invitation);
        databasePersistenceService.saveInvitation(invitation);
        GroupRecord group = groupsService.requireGroup(groupId);
        String actor = groupActivityService.displayName(requesterId);
        groupActivityService.log(groupId, requesterId, "INVITE_SENT", "GROUP_INVITATION", invitation.id(),
                null, invited.email(), actor + " invited " + displayName(invited) + " to " + group.name());
        inboxService.create(invited.id(), groupId, invitation.id(), "GROUP_INVITE", "Group invitation",
                actor + " invited you to join " + group.name() + ".");
        notificationService.send(invited, "BudgetBuddy Notification",
                actor + " invited you to join the BudgetBuddy group " + group.name() + ".");
        realtimeService.publishToUser(invited.id(), "group-invitation", toDto(invitation));
        return toDto(invitation);
    }

    public List<InvitationDto> listForUser(Long userId) {
        return store.groupInvitations.stream()
                .filter(invitation -> invitation.invitedUserId().equals(userId))
                .map(this::toDto)
                .toList();
    }

    public synchronized InvitationDto accept(Long userId, Long invitationId) {
        GroupInvitationRecord invitation = requireInvitationForUser(userId, invitationId);
        if (!"PENDING".equals(invitation.status())) {
            throw new DomainException(HttpStatus.CONFLICT, "Invitation already responded");
        }
        GroupInvitationRecord accepted = new GroupInvitationRecord(invitation.id(), invitation.groupId(), invitation.invitedUserId(),
                invitation.invitedByUserId(), "ACCEPTED", invitation.createdAt(), LocalDateTime.now());
        replaceInvitation(accepted);
        databasePersistenceService.saveInvitation(accepted);
        if (!groupsService.isMember(invitation.groupId(), userId)) {
            GroupMemberRecord member = new GroupMemberRecord(invitation.groupId(), userId, "MEMBER", false);
            store.members.add(member);
            databasePersistenceService.saveGroupMember(member);
        }
        GroupRecord group = groupsService.requireGroup(invitation.groupId());
        String actor = groupActivityService.displayName(userId);
        groupActivityService.log(invitation.groupId(), userId, "INVITE_ACCEPTED", "GROUP_INVITATION",
                invitation.id(), "PENDING", "ACCEPTED", actor + " accepted the group invitation");
        inboxService.markInvitationRead(userId, invitation.id());
        inboxService.create(userId, invitation.groupId(), invitation.id(), "GROUP_UPDATE",
                "Joined " + group.name(), "You joined " + group.name() + ".");
        notifyAcceptedMembers(invitation.groupId(), userId, actor + " accepted the invitation to " + group.name() + ".");
        realtimeService.publish("groups-updated", group);
        return toDto(requireInvitation(invitation.id()));
    }

    public synchronized InvitationDto decline(Long userId, Long invitationId) {
        GroupInvitationRecord invitation = requireInvitationForUser(userId, invitationId);
        if (!"PENDING".equals(invitation.status())) {
            throw new DomainException(HttpStatus.CONFLICT, "Invitation already responded");
        }
        GroupInvitationRecord declined = new GroupInvitationRecord(invitation.id(), invitation.groupId(), invitation.invitedUserId(),
                invitation.invitedByUserId(), "DECLINED", invitation.createdAt(), LocalDateTime.now());
        replaceInvitation(declined);
        databasePersistenceService.saveInvitation(declined);
        String actor = groupActivityService.displayName(userId);
        GroupRecord group = groupsService.requireGroup(invitation.groupId());
        groupActivityService.log(invitation.groupId(), userId, "INVITE_DECLINED", "GROUP_INVITATION",
                invitation.id(), "PENDING", "DECLINED", actor + " declined the group invitation");
        inboxService.markInvitationRead(userId, invitation.id());
        inboxService.create(userId, invitation.groupId(), invitation.id(), "GROUP_UPDATE",
                "Declined " + group.name(), "You declined the invitation to " + group.name() + ".");
        return toDto(requireInvitation(invitation.id()));
    }

    private void notifyAcceptedMembers(Long groupId, Long actorId, String message) {
        store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && !member.deleted()
                        && !member.userId().equals(actorId))
                .map(GroupMemberRecord::userId)
                .forEach(userId -> inboxService.create(userId, groupId, null, "GROUP_UPDATE",
                        "Group member joined", message));
    }

    private UserRecord findUserByEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        return store.users.stream()
                .filter(user -> user.email().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND,
                        "This user is not registered in BudgetBuddy."));
    }

    private boolean hasPendingInvitation(Long groupId, Long userId) {
        return store.groupInvitations.stream()
                .anyMatch(invitation -> invitation.groupId().equals(groupId)
                        && invitation.invitedUserId().equals(userId)
                        && "PENDING".equals(invitation.status()));
    }

    private GroupInvitationRecord requireInvitationForUser(Long userId, Long invitationId) {
        GroupInvitationRecord invitation = requireInvitation(invitationId);
        if (!invitation.invitedUserId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Only the invited user can respond");
        }
        return invitation;
    }

    private GroupInvitationRecord requireInvitation(Long invitationId) {
        return store.groupInvitations.stream()
                .filter(invitation -> invitation.id().equals(invitationId))
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Invitation not found"));
    }

    private void replaceInvitation(GroupInvitationRecord replacement) {
        for (int index = 0; index < store.groupInvitations.size(); index++) {
            if (store.groupInvitations.get(index).id().equals(replacement.id())) {
                store.groupInvitations.set(index, replacement);
                return;
            }
        }
    }

    private InvitationDto toDto(GroupInvitationRecord invitation) {
        GroupRecord group = groupsService.requireGroup(invitation.groupId());
        return new InvitationDto(invitation.id(), invitation.groupId(), group.name(), invitation.invitedUserId(),
                invitation.invitedByUserId(), groupActivityService.displayName(invitation.invitedByUserId()),
                invitation.status(), invitation.createdAt(), invitation.respondedAt());
    }

    private String displayName(UserRecord user) {
        String fullName = (user.firstname() + " " + user.lastname()).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        int at = user.email().indexOf('@');
        return at > 0 ? user.email().substring(0, at) : user.email();
    }
}
