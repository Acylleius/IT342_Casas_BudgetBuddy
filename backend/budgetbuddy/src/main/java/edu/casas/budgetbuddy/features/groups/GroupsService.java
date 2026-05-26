package edu.casas.budgetbuddy.features.groups;

import edu.casas.budgetbuddy.features.activity.ActivityService;
import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.groups.GroupsDtos.GroupDetailDto;
import edu.casas.budgetbuddy.features.notifications.NotificationService;
import edu.casas.budgetbuddy.features.realtime.RealtimeService;
import edu.casas.budgetbuddy.features.groups.GroupsDtos.GroupDto;
import edu.casas.budgetbuddy.features.groups.GroupsDtos.MemberDto;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupMemberRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GroupsService {
    private final BudgetBuddyStore store;
    private final AuthService authService;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;
    private final DatabasePersistenceService databasePersistenceService;

    public GroupsService(BudgetBuddyStore store, AuthService authService, ActivityService activityService,
                         NotificationService notificationService, RealtimeService realtimeService,
                         DatabasePersistenceService databasePersistenceService) {
        this.store = store;
        this.authService = authService;
        this.activityService = activityService;
        this.notificationService = notificationService;
        this.realtimeService = realtimeService;
        this.databasePersistenceService = databasePersistenceService;
    }

    public synchronized GroupDto create(Long creatorId, String name, String description) {
        GroupRecord group = new GroupRecord(store.groupIds.getAndIncrement(), name, description, creatorId, false);
        store.groups.add(group);
        GroupMemberRecord creator = new GroupMemberRecord(group.id(), creatorId, "ADMIN", false);
        store.members.add(creator);
        databasePersistenceService.saveGroup(group);
        databasePersistenceService.saveGroupMember(creator);
        activityService.log(creatorId, "CREATE_GROUP", "GROUP", group.id(), "Created group " + name);
        realtimeService.publish("groups-updated", toDto(group, "ADMIN"));
        return toDto(group, "ADMIN");
    }

    public List<GroupDto> list(Long userId) {
        return store.members.stream()
                .filter(member -> member.userId().equals(userId) && !member.deleted())
                .map(member -> store.groups.stream()
                        .filter(group -> group.id().equals(member.groupId()) && !group.deleted())
                        .findFirst()
                        .map(group -> toDto(group, member.role()))
                        .orElse(null))
                .filter(group -> group != null)
                .toList();
    }

    public GroupDetailDto detail(Long userId, Long groupId) {
        GroupRecord group = requireGroup(groupId);
        requireMember(groupId, userId);
        return toDetail(group);
    }

    public synchronized GroupDto update(Long userId, Long groupId, String name, String description) {
        requireAdmin(groupId, userId);
        GroupRecord current = requireGroup(groupId);
        GroupRecord replacement = new GroupRecord(current.id(), name, description, current.createdBy(), current.deleted());
        replaceGroup(replacement);
        databasePersistenceService.saveGroup(replacement);
        activityService.log(userId, "UPDATE_GROUP", "GROUP", groupId, "Updated group " + name);
        realtimeService.publish("groups-updated", toDto(replacement, "ADMIN"));
        return toDto(replacement, "ADMIN");
    }

    public synchronized void addMember(Long adminId, Long groupId, String email) {
        requireAdmin(groupId, adminId);
        UserRecord user = authService.findByEmail(email)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "User not found"));
        if (isMember(groupId, user.id())) {
            throw new DomainException(HttpStatus.CONFLICT, "User is already a member");
        }
        GroupMemberRecord member = new GroupMemberRecord(groupId, user.id(), "MEMBER", false);
        store.members.add(member);
        databasePersistenceService.saveGroupMember(member);
        GroupRecord group = requireGroup(groupId);
        activityService.log(adminId, "ADD_GROUP_MEMBER", "GROUP", groupId,
                "Added " + user.email() + " to " + group.name());
        notificationService.send(user, "BudgetBuddy Notification",
                "You were added to the BudgetBuddy group " + group.name() + ".");
        realtimeService.publish("groups-updated", toDetail(group));
    }

    public synchronized void removeMember(Long adminId, Long groupId, Long memberUserId) {
        requireAdmin(groupId, adminId);
        GroupMemberRecord member = requireMember(groupId, memberUserId);
        if ("ADMIN".equals(member.role()) && activeAdmins(groupId) <= 1) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Cannot remove the last admin");
        }
        replaceMember(new GroupMemberRecord(groupId, memberUserId, member.role(), true));
        activityService.log(adminId, "REMOVE_GROUP_MEMBER", "GROUP", groupId,
                "Removed user #" + memberUserId + " from group #" + groupId);
        realtimeService.publish("groups-updated", toDetail(requireGroup(groupId)));
    }

    public synchronized void softDelete(Long userId, Long groupId) {
        requireAdmin(groupId, userId);
        GroupRecord group = requireGroup(groupId);
        replaceGroup(new GroupRecord(group.id(), group.name(), group.description(), group.createdBy(), true));
        activityService.log(userId, "DELETE_GROUP", "GROUP", groupId, "Deleted group " + group.name());
        realtimeService.publish("groups-updated", toDto(group, "ADMIN"));
    }

    public boolean isMember(Long groupId, Long userId) {
        return store.members.stream()
                .anyMatch(member -> member.groupId().equals(groupId) && member.userId().equals(userId) && !member.deleted());
    }

    public GroupMemberRecord requireMember(Long groupId, Long userId) {
        return store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && member.userId().equals(userId) && !member.deleted())
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "Group membership required"));
    }

    public void requireAdmin(Long groupId, Long userId) {
        GroupMemberRecord member = requireMember(groupId, userId);
        if (!"ADMIN".equals(member.role())) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    public GroupRecord requireGroup(Long groupId) {
        return store.groups.stream()
                .filter(group -> group.id().equals(groupId) && !group.deleted())
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private GroupDetailDto toDetail(GroupRecord group) {
        List<MemberDto> members = store.members.stream()
                .filter(member -> member.groupId().equals(group.id()) && !member.deleted())
                .map(member -> store.users.stream()
                        .filter(user -> user.id().equals(member.userId()))
                        .findFirst()
                        .map(user -> new MemberDto(user.id(), user.email(), user.firstname(), user.lastname(), member.role()))
                        .orElse(null))
                .filter(member -> member != null)
                .toList();
        return new GroupDetailDto(group.id(), group.name(), group.description(), group.createdBy(), members);
    }

    private GroupDto toDto(GroupRecord group, String role) {
        return new GroupDto(group.id(), group.name(), group.description(), group.createdBy(), role);
    }

    private long activeAdmins(Long groupId) {
        return store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && "ADMIN".equals(member.role()) && !member.deleted())
                .count();
    }

    private void replaceGroup(GroupRecord replacement) {
        for (int index = 0; index < store.groups.size(); index++) {
            if (store.groups.get(index).id().equals(replacement.id())) {
                store.groups.set(index, replacement);
                return;
            }
        }
    }

    private void replaceMember(GroupMemberRecord replacement) {
        for (int index = 0; index < store.members.size(); index++) {
            GroupMemberRecord current = store.members.get(index);
            if (current.groupId().equals(replacement.groupId()) && current.userId().equals(replacement.userId())) {
                store.members.set(index, replacement);
                return;
            }
        }
    }
}
