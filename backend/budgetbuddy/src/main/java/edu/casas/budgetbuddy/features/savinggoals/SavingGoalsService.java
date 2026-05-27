package edu.casas.budgetbuddy.features.savinggoals;

import edu.casas.budgetbuddy.features.groups.GroupsService;
import edu.casas.budgetbuddy.features.inbox.InboxService;
import edu.casas.budgetbuddy.features.savinggoals.SavingGoalsDtos.ContributionDto;
import edu.casas.budgetbuddy.features.savinggoals.SavingGoalsDtos.ContributionRequest;
import edu.casas.budgetbuddy.features.savinggoals.SavingGoalsDtos.SavingGoalDto;
import edu.casas.budgetbuddy.features.savinggoals.SavingGoalsDtos.SavingGoalRequest;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.SavingGoalContributionRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.SavingGoalRecord;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SavingGoalsService {
    private final BudgetBuddyStore store;
    private final GroupsService groupsService;
    private final InboxService inboxService;
    private final DatabasePersistenceService databasePersistenceService;
    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"));

    public SavingGoalsService(BudgetBuddyStore store, GroupsService groupsService, InboxService inboxService,
                              DatabasePersistenceService databasePersistenceService) {
        this.store = store;
        this.groupsService = groupsService;
        this.inboxService = inboxService;
        this.databasePersistenceService = databasePersistenceService;
    }

    public synchronized SavingGoalDto createPersonal(Long userId, SavingGoalRequest request) {
        SavingGoalRecord record = new SavingGoalRecord(store.savingGoalIds.getAndIncrement(), "PERSONAL",
                userId, null, userId, request.title(), request.targetAmount(), request.currentAmount(),
                request.deadline(), false, false, LocalDateTime.now(), LocalDateTime.now());
        store.savingGoals.add(record);
        databasePersistenceService.saveSavingGoal(record);
        notifyIfCompleted(record);
        return toDto(requireGoal(record.id()));
    }

    public List<SavingGoalDto> listPersonal(Long userId) {
        return store.savingGoals.stream()
                .filter(goal -> !goal.deleted() && "PERSONAL".equals(goal.scope()) && goal.userId().equals(userId))
                .map(this::toDto)
                .toList();
    }

    public synchronized SavingGoalDto updatePersonal(Long userId, Long goalId, SavingGoalRequest request) {
        SavingGoalRecord current = requireGoal(goalId);
        if (!"PERSONAL".equals(current.scope()) || !current.userId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Saving goal access denied");
        }
        SavingGoalRecord replacement = replaceGoal(current, request);
        notifyIfCompleted(replacement);
        return toDto(requireGoal(goalId));
    }

    public synchronized void deletePersonal(Long userId, Long goalId) {
        SavingGoalRecord current = requireGoal(goalId);
        if (!"PERSONAL".equals(current.scope()) || !current.userId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Saving goal access denied");
        }
        softDelete(current);
    }

    public synchronized SavingGoalDto createGroup(Long userId, Long groupId, SavingGoalRequest request) {
        groupsService.requireMember(groupId, userId);
        SavingGoalRecord record = new SavingGoalRecord(store.savingGoalIds.getAndIncrement(), "GROUP",
                null, groupId, userId, request.title(), request.targetAmount(), request.currentAmount(),
                request.deadline(), false, false, LocalDateTime.now(), LocalDateTime.now());
        store.savingGoals.add(record);
        databasePersistenceService.saveSavingGoal(record);
        notifyIfCompleted(record);
        return toDto(requireGoal(record.id()));
    }

    public List<SavingGoalDto> listGroup(Long userId, Long groupId) {
        groupsService.requireMember(groupId, userId);
        return store.savingGoals.stream()
                .filter(goal -> !goal.deleted() && "GROUP".equals(goal.scope()) && goal.groupId().equals(groupId))
                .map(this::toDto)
                .toList();
    }

    public synchronized SavingGoalDto updateGroup(Long userId, Long groupId, Long goalId, SavingGoalRequest request) {
        groupsService.requireMember(groupId, userId);
        SavingGoalRecord current = requireGoal(goalId);
        if (!"GROUP".equals(current.scope()) || !current.groupId().equals(groupId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Saving goal not found");
        }
        SavingGoalRecord replacement = replaceGoal(current, request);
        notifyIfCompleted(replacement);
        return toDto(requireGoal(goalId));
    }

    public synchronized void deleteGroup(Long userId, Long groupId, Long goalId) {
        groupsService.requireMember(groupId, userId);
        SavingGoalRecord current = requireGoal(goalId);
        if (!"GROUP".equals(current.scope()) || !current.groupId().equals(groupId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Saving goal not found");
        }
        softDelete(current);
    }

    public synchronized SavingGoalDto contribute(Long userId, Long groupId, Long goalId, ContributionRequest request) {
        groupsService.requireMember(groupId, userId);
        SavingGoalRecord current = requireGoal(goalId);
        if (!"GROUP".equals(current.scope()) || !current.groupId().equals(groupId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Saving goal not found");
        }
        SavingGoalContributionRecord contribution = new SavingGoalContributionRecord(
                store.savingGoalContributionIds.getAndIncrement(), goalId, userId, request.amount(),
                request.note(), LocalDateTime.now());
        store.savingGoalContributions.add(contribution);
        databasePersistenceService.saveSavingGoalContribution(contribution);
        SavingGoalRecord replacement = new SavingGoalRecord(current.id(), current.scope(), current.userId(),
                current.groupId(), current.createdByUserId(), current.title(), current.targetAmount(),
                current.currentAmount().add(request.amount()), current.deadline(), false,
                current.completionNotified(), current.createdAt(), LocalDateTime.now());
        replace(replacement);
        databasePersistenceService.saveSavingGoal(replacement);
        notifyIfCompleted(replacement);
        return toDto(requireGoal(goalId));
    }

    private void notifyIfCompleted(SavingGoalRecord goal) {
        if (goal.completionNotified() || goal.currentAmount().compareTo(goal.targetAmount()) < 0) {
            return;
        }
        SavingGoalRecord notified = new SavingGoalRecord(goal.id(), goal.scope(), goal.userId(), goal.groupId(),
                goal.createdByUserId(), goal.title(), goal.targetAmount(), goal.currentAmount(), goal.deadline(),
                goal.deleted(), true, goal.createdAt(), LocalDateTime.now());
        replace(notified);
        databasePersistenceService.saveSavingGoal(notified);
        if ("PERSONAL".equals(goal.scope())) {
            inboxService.create(goal.userId(), null, null, "SAVING_GOAL_COMPLETED",
                    "Saving Goal Completed", goal.title() + " reached " + pesoFormat.format(goal.targetAmount()) + ".");
            return;
        }
        String groupName = store.groups.stream()
                .filter(group -> group.id().equals(goal.groupId()))
                .map(BudgetBuddyStore.GroupRecord::name)
                .findFirst()
                .orElse("Group");
        String message = groupName + " completed " + goal.title() + " with "
                + pesoFormat.format(goal.currentAmount()) + ".";
        store.members.stream()
                .filter(member -> member.groupId().equals(goal.groupId()) && !member.deleted())
                .forEach(member -> inboxService.create(member.userId(), goal.groupId(), null,
                        "GROUP_SAVING_GOAL_COMPLETED", "Group Saving Goal Completed", message));
    }

    private SavingGoalRecord replaceGoal(SavingGoalRecord current, SavingGoalRequest request) {
        boolean completionNotified = current.completionNotified()
                && request.currentAmount().compareTo(request.targetAmount()) >= 0;
        SavingGoalRecord replacement = new SavingGoalRecord(current.id(), current.scope(), current.userId(),
                current.groupId(), current.createdByUserId(), request.title(), request.targetAmount(),
                request.currentAmount(), request.deadline(), false, completionNotified,
                current.createdAt(), LocalDateTime.now());
        replace(replacement);
        databasePersistenceService.saveSavingGoal(replacement);
        return replacement;
    }

    private void softDelete(SavingGoalRecord current) {
        SavingGoalRecord replacement = new SavingGoalRecord(current.id(), current.scope(), current.userId(),
                current.groupId(), current.createdByUserId(), current.title(), current.targetAmount(),
                current.currentAmount(), current.deadline(), true, current.completionNotified(),
                current.createdAt(), LocalDateTime.now());
        replace(replacement);
        databasePersistenceService.saveSavingGoal(replacement);
    }

    private SavingGoalRecord requireGoal(Long goalId) {
        return store.savingGoals.stream()
                .filter(goal -> goal.id().equals(goalId) && !goal.deleted())
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Saving goal not found"));
    }

    private void replace(SavingGoalRecord replacement) {
        for (int index = 0; index < store.savingGoals.size(); index++) {
            if (store.savingGoals.get(index).id().equals(replacement.id())) {
                store.savingGoals.set(index, replacement);
                return;
            }
        }
    }

    private SavingGoalDto toDto(SavingGoalRecord goal) {
        BigDecimal remaining = goal.targetAmount().subtract(goal.currentAmount()).max(BigDecimal.ZERO);
        BigDecimal percentage = goal.targetAmount().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : goal.currentAmount().multiply(BigDecimal.valueOf(100)).divide(goal.targetAmount(), 2, RoundingMode.HALF_UP);
        String status = goal.currentAmount().compareTo(goal.targetAmount()) >= 0 ? "COMPLETED"
                : goal.deadline() != null && goal.deadline().isBefore(LocalDate.now()) ? "OVERDUE" : "IN_PROGRESS";
        return new SavingGoalDto(goal.id(), goal.scope(), goal.userId(), goal.groupId(), goal.createdByUserId(),
                goal.title(), goal.targetAmount(), goal.currentAmount(), remaining, percentage, goal.deadline(),
                status, goal.createdAt(), goal.updatedAt(), contributions(goal.id()));
    }

    private List<ContributionDto> contributions(Long goalId) {
        return store.savingGoalContributions.stream()
                .filter(contribution -> contribution.savingGoalId().equals(goalId))
                .map(contribution -> new ContributionDto(contribution.id(), contribution.userId(),
                        contribution.amount(), contribution.note(), contribution.createdAt()))
                .toList();
    }
}
