package edu.casas.budgetbuddy.features.sharedexpenses;

import edu.casas.budgetbuddy.features.activity.ActivityService;
import edu.casas.budgetbuddy.features.budgets.BudgetsService;
import edu.casas.budgetbuddy.features.groups.GroupsService;
import edu.casas.budgetbuddy.features.notifications.NotificationService;
import edu.casas.budgetbuddy.features.realtime.RealtimeService;
import edu.casas.budgetbuddy.features.sharedexpenses.SharedExpensesDtos.BalanceDto;
import edu.casas.budgetbuddy.features.sharedexpenses.SharedExpensesDtos.BalanceExpense;
import edu.casas.budgetbuddy.features.sharedexpenses.SharedExpensesDtos.BalanceMember;
import edu.casas.budgetbuddy.features.sharedexpenses.SharedExpensesDtos.SharedExpenseDto;
import edu.casas.budgetbuddy.features.sharedexpenses.SharedExpensesDtos.SplitDto;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.ExpenseSplitRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.SharedExpenseRecord;
import edu.casas.budgetbuddy.shared.utils.CategoryUtils;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SharedExpensesService {
    private final BudgetBuddyStore store;
    private final GroupsService groupsService;
    private final ActivityService activityService;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;
    private final BudgetsService budgetsService;
    private final NumberFormat pesoFormat;

    public SharedExpensesService(BudgetBuddyStore store, GroupsService groupsService,
                                 ActivityService activityService, NotificationService notificationService,
                                 RealtimeService realtimeService, BudgetsService budgetsService) {
        this.store = store;
        this.groupsService = groupsService;
        this.activityService = activityService;
        this.notificationService = notificationService;
        this.realtimeService = realtimeService;
        this.budgetsService = budgetsService;
        this.pesoFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"));
    }

    public synchronized SharedExpenseDto create(Long requesterId, Long groupId, BigDecimal amount, String category,
                                                String description, Long paidBy, LocalDate expenseDate,
                                                List<Long> participantUserIds) {
        groupsService.requireMember(groupId, requesterId);
        groupsService.requireMember(groupId, paidBy);
        String normalizedCategory = CategoryUtils.require(category);
        LocalDate actualDate = expenseDate == null ? LocalDate.now() : expenseDate;
        if (actualDate.isAfter(LocalDate.now())) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "Expense date cannot be in the future");
        }
        List<Long> participants = participantUserIds == null || participantUserIds.isEmpty()
                ? activeMemberIds(groupId)
                : participantUserIds;
        participants.forEach(userId -> groupsService.requireMember(groupId, userId));
        BigDecimal splitAmount = amount.divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);

        SharedExpenseRecord expense = new SharedExpenseRecord(store.expenseIds.getAndIncrement(), groupId,
                paidBy, amount, normalizedCategory, description, actualDate, false);
        store.expenses.add(expense);
        participants.forEach(userId -> store.splits.add(new ExpenseSplitRecord(store.splitIds.getAndIncrement(),
                expense.id(), userId, splitAmount, userId.equals(paidBy), userId.equals(paidBy) ? LocalDateTime.now() : null,
                false)));
        String descriptionText = actorName(requesterId) + " added " + normalizedCategory + " expense " + pesoFormat.format(amount);
        activityService.log(requesterId, "CREATE_SHARED_EXPENSE", "SHARED_EXPENSE", expense.id(), descriptionText);
        notificationService.notifyGroupMembers(requesterId, groupId, notificationMessage(groupId,
                actorName(requesterId) + " created a shared expense", normalizedCategory, null, amount));
        budgetsService.evaluateGroupBudgets(groupId);
        realtimeService.publish("shared-expenses-updated", toDto(expense));
        return toDto(expense);
    }

    public List<SharedExpenseDto> list(Long requesterId, Long groupId) {
        groupsService.requireMember(groupId, requesterId);
        return store.expenses.stream()
                .filter(expense -> expense.groupId().equals(groupId) && !expense.deleted())
                .map(this::toDto)
                .toList();
    }

    public List<SharedExpenseDto> listForUser(Long requesterId) {
        return store.expenses.stream()
                .filter(expense -> !expense.deleted() && groupsService.isMember(expense.groupId(), requesterId))
                .map(this::toDto)
                .toList();
    }

    public List<BalanceDto> balances(Long requesterId, Long groupId) {
        groupsService.requireMember(groupId, requesterId);
        List<BalanceExpense> expenses = store.expenses.stream()
                .filter(expense -> expense.groupId().equals(groupId) && !expense.deleted())
                .map(expense -> new BalanceExpense(expense.amount(), expense.paidBy()))
                .toList();
        List<BalanceMember> members = activeMemberIds(groupId).stream().map(BalanceMember::new).toList();
        return calculateBalances(expenses, members);
    }

    public Map<Long, List<BalanceDto>> balancesForUser(Long requesterId) {
        Map<Long, List<BalanceDto>> result = new LinkedHashMap<>();
        store.members.stream()
                .filter(member -> member.userId().equals(requesterId) && !member.deleted())
                .map(BudgetBuddyStore.GroupMemberRecord::groupId)
                .distinct()
                .forEach(groupId -> result.put(groupId, balances(requesterId, groupId)));
        return result;
    }

    public synchronized SharedExpenseDto update(Long requesterId, Long expenseId, BigDecimal amount,
                                                String category, String description, LocalDate expenseDate) {
        SharedExpenseRecord expense = requireExpense(expenseId);
        String normalizedCategory = CategoryUtils.require(category);
        boolean admin = false;
        try {
            groupsService.requireAdmin(expense.groupId(), requesterId);
            admin = true;
        } catch (DomainException ignored) {
            admin = false;
        }
        if (!admin && !expense.paidBy().equals(requesterId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Only the payer or an admin can update this expense");
        }
        LocalDate actualDate = expenseDate == null ? expense.expenseDate() : expenseDate;
        SharedExpenseRecord replacement = new SharedExpenseRecord(expense.id(), expense.groupId(), expense.paidBy(),
                amount, normalizedCategory, description, actualDate, false);
        replaceExpense(replacement);
        activityService.log(requesterId, "UPDATE_SHARED_EXPENSE", "SHARED_EXPENSE", expense.id(),
                actorName(requesterId) + " updated " + normalizedCategory + " from " + pesoFormat.format(expense.amount())
                        + " to " + pesoFormat.format(amount));
        notificationService.notifyGroupMembers(requesterId, expense.groupId(), notificationMessage(expense.groupId(),
                actorName(requesterId) + " updated a shared expense", normalizedCategory, expense.amount(), amount));
        budgetsService.evaluateGroupBudgets(expense.groupId());
        realtimeService.publish("shared-expenses-updated", toDto(replacement));
        return toDto(replacement);
    }

    public synchronized void softDelete(Long requesterId, Long expenseId) {
        SharedExpenseRecord expense = requireExpense(expenseId);
        boolean admin = false;
        try {
            groupsService.requireAdmin(expense.groupId(), requesterId);
            admin = true;
        } catch (DomainException ignored) {
            admin = false;
        }
        if (!admin && !expense.paidBy().equals(requesterId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Only the payer or an admin can delete this expense");
        }
        replaceExpense(new SharedExpenseRecord(expense.id(), expense.groupId(), expense.paidBy(), expense.amount(),
                expense.category(), expense.description(), expense.expenseDate(), true));
        activityService.log(requesterId, "DELETE_SHARED_EXPENSE", "SHARED_EXPENSE", expenseId,
                actorName(requesterId) + " deleted shared expense " + expense.category());
        budgetsService.evaluateGroupBudgets(expense.groupId());
        realtimeService.publish("shared-expenses-updated", toDto(expense));
    }

    public synchronized void settle(Long requesterId, Long splitId) {
        for (int index = 0; index < store.splits.size(); index++) {
            ExpenseSplitRecord split = store.splits.get(index);
            if (split.id().equals(splitId) && !split.deleted()) {
                if (!split.userId().equals(requesterId)) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "Cannot settle another user's split");
                }
                store.splits.set(index, new ExpenseSplitRecord(split.id(), split.expenseId(), split.userId(),
                        split.amount(), true, LocalDateTime.now(), split.deleted()));
                SharedExpenseRecord expense = requireExpense(split.expenseId());
                activityService.log(requesterId, "SETTLE_SPLIT", "SHARED_EXPENSE", expense.id(),
                        actorName(requesterId) + " settled balance for " + expense.category());
                notificationService.notifyGroupMembers(requesterId, expense.groupId(),
                        actorName(requesterId) + " settled a balance in " + groupName(expense.groupId()));
                realtimeService.publish("shared-expenses-updated", toDto(expense));
                return;
            }
        }
        throw new DomainException(HttpStatus.NOT_FOUND, "Split not found");
    }

    public static List<BalanceDto> calculateBalances(List<BalanceExpense> expenses, List<BalanceMember> members) {
        Map<Long, BalanceAccumulator> balances = new LinkedHashMap<>();
        members.forEach(member -> balances.put(member.userId(), new BalanceAccumulator()));
        for (BalanceExpense expense : expenses) {
            if (members.isEmpty()) {
                continue;
            }
            BigDecimal owedPerMember = expense.amount().divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
            balances.computeIfAbsent(expense.paidBy(), ignored -> new BalanceAccumulator()).paid =
                    balances.computeIfAbsent(expense.paidBy(), ignored -> new BalanceAccumulator()).paid.add(expense.amount());
            members.forEach(member -> balances.get(member.userId()).owed =
                    balances.get(member.userId()).owed.add(owedPerMember));
        }
        return balances.entrySet().stream()
                .map(entry -> new BalanceDto(entry.getKey(), entry.getValue().paid,
                        entry.getValue().owed, entry.getValue().paid.subtract(entry.getValue().owed)))
                .toList();
    }

    private List<Long> activeMemberIds(Long groupId) {
        return store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && !member.deleted())
                .map(member -> member.userId())
                .toList();
    }

    private SharedExpenseRecord requireExpense(Long expenseId) {
        return store.expenses.stream()
                .filter(expense -> expense.id().equals(expenseId) && !expense.deleted())
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private SharedExpenseDto toDto(SharedExpenseRecord expense) {
        List<SplitDto> splits = store.splits.stream()
                .filter(split -> split.expenseId().equals(expense.id()) && !split.deleted())
                .map(split -> new SplitDto(split.id(), split.userId(), split.amount(), split.settled()))
                .toList();
        return new SharedExpenseDto(expense.id(), expense.groupId(), expense.paidBy(), expense.amount(),
                expense.category(), expense.description(), expense.expenseDate(), splits);
    }

    private void replaceExpense(SharedExpenseRecord replacement) {
        for (int index = 0; index < store.expenses.size(); index++) {
            if (store.expenses.get(index).id().equals(replacement.id())) {
                store.expenses.set(index, replacement);
                return;
            }
        }
    }

    private String notificationMessage(Long groupId, String heading, String expense, BigDecimal oldAmount, BigDecimal newAmount) {
        return """
                %s in %s

                Expense:
                %s

                Old amount:
                %s

                New amount:
                %s

                Time:
                %s
                """.formatted(heading, groupName(groupId), expense,
                oldAmount == null ? "N/A" : pesoFormat.format(oldAmount),
                newAmount == null ? "N/A" : pesoFormat.format(newAmount),
                LocalDateTime.now());
    }

    private String actorName(Long userId) {
        return store.users.stream()
                .filter(user -> user.id().equals(userId))
                .map(user -> user.firstname() + " " + user.lastname())
                .findFirst()
                .orElse("User #" + userId);
    }

    private String groupName(Long groupId) {
        return store.groups.stream()
                .filter(group -> group.id().equals(groupId))
                .map(BudgetBuddyStore.GroupRecord::name)
                .findFirst()
                .orElse("Group #" + groupId);
    }

    private static final class BalanceAccumulator {
        private BigDecimal paid = BigDecimal.ZERO;
        private BigDecimal owed = BigDecimal.ZERO;
    }
}
