package edu.casas.budgetbuddy.features.grouptransactions;

import edu.casas.budgetbuddy.features.budgets.BudgetsService;
import edu.casas.budgetbuddy.features.groups.GroupActivityService;
import edu.casas.budgetbuddy.features.groups.GroupsService;
import edu.casas.budgetbuddy.features.grouptransactions.GroupTransactionsDtos.GroupSummaryDto;
import edu.casas.budgetbuddy.features.grouptransactions.GroupTransactionsDtos.GroupTransactionDto;
import edu.casas.budgetbuddy.features.inbox.InboxService;
import edu.casas.budgetbuddy.features.notifications.NotificationService;
import edu.casas.budgetbuddy.features.realtime.RealtimeService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.ExpenseSplitRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupMemberRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupTransactionRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.SharedExpenseRecord;
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
public class GroupTransactionsService {
    private final BudgetBuddyStore store;
    private final GroupsService groupsService;
    private final GroupActivityService groupActivityService;
    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;
    private final DatabasePersistenceService databasePersistenceService;
    private final BudgetsService budgetsService;
    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"));

    public GroupTransactionsService(BudgetBuddyStore store, GroupsService groupsService,
                                    GroupActivityService groupActivityService, InboxService inboxService,
                                    NotificationService notificationService, RealtimeService realtimeService,
                                    DatabasePersistenceService databasePersistenceService,
                                    BudgetsService budgetsService) {
        this.store = store;
        this.groupsService = groupsService;
        this.groupActivityService = groupActivityService;
        this.inboxService = inboxService;
        this.notificationService = notificationService;
        this.realtimeService = realtimeService;
        this.databasePersistenceService = databasePersistenceService;
        this.budgetsService = budgetsService;
    }

    public synchronized GroupTransactionDto create(Long requesterId, Long groupId, String type, BigDecimal amount,
                                                   String category, String description, Long actorUserId,
                                                   LocalDate transactionDate) {
        groupsService.requireMember(groupId, requesterId);
        Long actor = actorUserId == null ? requesterId : actorUserId;
        groupsService.requireMember(groupId, actor);
        String normalizedType = normalizeType(type);
        LocalDate actualDate = transactionDate == null ? LocalDate.now() : transactionDate;
        Long sharedExpenseId = null;
        if ("EXPENSE".equals(normalizedType)) {
            sharedExpenseId = createBalanceExpense(groupId, actor, amount, category, description, actualDate);
        }
        GroupTransactionRecord record = new GroupTransactionRecord(store.groupTransactionIds.getAndIncrement(),
                groupId, actor, normalizedType, amount, category, description, actualDate,
                LocalDateTime.now(), LocalDateTime.now(), sharedExpenseId, false);
        store.groupTransactions.add(record);
        databasePersistenceService.saveGroupTransaction(record);

        String action = "INCOME".equals(normalizedType) ? "GROUP_INCOME_CREATED" : "GROUP_EXPENSE_CREATED";
        String noun = "INCOME".equals(normalizedType) ? "Income" : "Expense";
        String descriptionText = groupActivityService.displayName(actor) + " added " + noun + " "
                + category + " " + pesoFormat.format(amount);
        groupActivityService.log(groupId, actor, action, "GROUP_TRANSACTION", record.id(),
                null, pesoFormat.format(amount), descriptionText);
        notifyGroup(groupId, actor, "Group " + noun.toLowerCase() + " added", descriptionText,
                "INCOME".equals(normalizedType) ? "GROUP_INCOME_CREATED" : "GROUP_EXPENSE_CREATED");
        GroupTransactionDto dto = toDto(record);
        budgetsService.evaluateGroupBudgets(groupId);
        realtimeService.publishToUsers(activeMemberIds(groupId), "group-transaction-updated", dto);
        return dto;
    }

    public List<GroupTransactionDto> list(Long requesterId, Long groupId) {
        groupsService.requireMember(groupId, requesterId);
        return store.groupTransactions.stream()
                .filter(record -> record.groupId().equals(groupId) && !record.deleted())
                .map(this::toDto)
                .toList();
    }

    public GroupSummaryDto summary(Long requesterId, Long groupId) {
        groupsService.requireMember(groupId, requesterId);
        List<GroupTransactionRecord> records = store.groupTransactions.stream()
                .filter(record -> record.groupId().equals(groupId) && !record.deleted())
                .toList();
        BigDecimal income = sum(records, "INCOME");
        BigDecimal expenses = sum(records, "EXPENSE");
        return new GroupSummaryDto(income, expenses, income.subtract(expenses));
    }

    public synchronized GroupTransactionDto update(Long requesterId, Long groupId, Long transactionId,
                                                   String type, BigDecimal amount, String category,
                                                   String description, LocalDate transactionDate) {
        groupsService.requireMember(groupId, requesterId);
        String normalizedType = normalizeType(type);
        for (int index = 0; index < store.groupTransactions.size(); index++) {
            GroupTransactionRecord current = store.groupTransactions.get(index);
            if (current.id().equals(transactionId) && current.groupId().equals(groupId) && !current.deleted()) {
                if (!current.createdByUserId().equals(requesterId)) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "Only the creator can update this group transaction");
                }
                GroupTransactionRecord replacement = new GroupTransactionRecord(current.id(), current.groupId(),
                        current.createdByUserId(), normalizedType, amount, category, description,
                        transactionDate == null ? current.transactionDate() : transactionDate,
                        current.createdAt(), LocalDateTime.now(), current.sharedExpenseId(), false);
                store.groupTransactions.set(index, replacement);
                databasePersistenceService.saveGroupTransaction(replacement);
                updateLinkedExpense(current.sharedExpenseId(), amount, category, description, replacement.transactionDate());
                String oldValue = current.category() + " " + pesoFormat.format(current.amount());
                String newValue = category + " " + pesoFormat.format(amount);
                String descriptionText = groupActivityService.displayName(requesterId) + " updated "
                        + category + " from " + pesoFormat.format(current.amount()) + " to " + pesoFormat.format(amount);
                groupActivityService.log(groupId, requesterId,
                        "INCOME".equals(normalizedType) ? "GROUP_INCOME_UPDATED" : "GROUP_EXPENSE_UPDATED",
                        "GROUP_TRANSACTION", transactionId, oldValue, newValue, descriptionText);
                notifyGroup(groupId, requesterId, "Group transaction updated", descriptionText,
                        "GROUP_UPDATE");
                GroupTransactionDto dto = toDto(replacement);
                budgetsService.evaluateGroupBudgets(groupId);
                realtimeService.publishToUsers(activeMemberIds(groupId), "group-transaction-updated", dto);
                return dto;
            }
        }
        throw new DomainException(HttpStatus.NOT_FOUND, "Group transaction not found");
    }

    public synchronized void delete(Long requesterId, Long groupId, Long transactionId) {
        groupsService.requireMember(groupId, requesterId);
        for (int index = 0; index < store.groupTransactions.size(); index++) {
            GroupTransactionRecord current = store.groupTransactions.get(index);
            if (current.id().equals(transactionId) && current.groupId().equals(groupId) && !current.deleted()) {
                if (!current.createdByUserId().equals(requesterId)) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "Only the creator can delete this group transaction");
                }
                store.groupTransactions.set(index, new GroupTransactionRecord(current.id(), current.groupId(),
                        current.createdByUserId(), current.type(), current.amount(), current.category(),
                        current.description(), current.transactionDate(), current.createdAt(),
                        LocalDateTime.now(), current.sharedExpenseId(), true));
                softDeleteLinkedExpense(current.sharedExpenseId());
                String action = "INCOME".equals(current.type()) ? "GROUP_INCOME_DELETED" : "GROUP_EXPENSE_DELETED";
                String descriptionText = groupActivityService.displayName(requesterId) + " deleted "
                        + current.category() + " " + pesoFormat.format(current.amount());
                groupActivityService.log(groupId, requesterId, action, "GROUP_TRANSACTION", transactionId,
                        current.category(), null, descriptionText);
                notifyGroup(groupId, requesterId, "Group transaction deleted", descriptionText, "GROUP_UPDATE");
                budgetsService.evaluateGroupBudgets(groupId);
                realtimeService.publishToUsers(activeMemberIds(groupId), "group-transaction-updated", toDto(current));
                return;
            }
        }
        throw new DomainException(HttpStatus.NOT_FOUND, "Group transaction not found");
    }

    private Long createBalanceExpense(Long groupId, Long paidBy, BigDecimal amount, String category,
                                      String description, LocalDate date) {
        List<Long> participants = activeMemberIds(groupId);
        BigDecimal splitAmount = amount.divide(BigDecimal.valueOf(participants.size()), 2, RoundingMode.HALF_UP);
        SharedExpenseRecord expense = new SharedExpenseRecord(store.expenseIds.getAndIncrement(), groupId,
                paidBy, amount, category, description, date, false);
        store.expenses.add(expense);
        participants.forEach(userId -> store.splits.add(new ExpenseSplitRecord(store.splitIds.getAndIncrement(),
                expense.id(), userId, splitAmount, userId.equals(paidBy),
                userId.equals(paidBy) ? LocalDateTime.now() : null, false)));
        return expense.id();
    }

    private void updateLinkedExpense(Long expenseId, BigDecimal amount, String category,
                                     String description, LocalDate date) {
        if (expenseId == null) {
            return;
        }
        for (int index = 0; index < store.expenses.size(); index++) {
            SharedExpenseRecord expense = store.expenses.get(index);
            if (expense.id().equals(expenseId)) {
                store.expenses.set(index, new SharedExpenseRecord(expense.id(), expense.groupId(), expense.paidBy(),
                        amount, category, description, date, expense.deleted()));
                return;
            }
        }
    }

    private void softDeleteLinkedExpense(Long expenseId) {
        if (expenseId == null) {
            return;
        }
        for (int index = 0; index < store.expenses.size(); index++) {
            SharedExpenseRecord expense = store.expenses.get(index);
            if (expense.id().equals(expenseId)) {
                store.expenses.set(index, new SharedExpenseRecord(expense.id(), expense.groupId(), expense.paidBy(),
                        expense.amount(), expense.category(), expense.description(), expense.expenseDate(), true));
                return;
            }
        }
    }

    private void notifyGroup(Long groupId, Long actorId, String title, String message, String type) {
        store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && !member.deleted()
                        && !member.userId().equals(actorId))
                .map(GroupMemberRecord::userId)
                .forEach(userId -> inboxService.create(userId, groupId, null, type, title, message));
        notificationService.notifyGroupMembers(actorId, groupId, message);
    }

    private List<Long> activeMemberIds(Long groupId) {
        return store.members.stream()
                .filter(member -> member.groupId().equals(groupId) && !member.deleted())
                .map(GroupMemberRecord::userId)
                .toList();
    }

    private BigDecimal sum(List<GroupTransactionRecord> records, String type) {
        return records.stream()
                .filter(record -> record.type().equals(type))
                .map(GroupTransactionRecord::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!normalized.equals("INCOME") && !normalized.equals("EXPENSE")) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "type must be INCOME or EXPENSE");
        }
        return normalized;
    }

    private GroupTransactionDto toDto(GroupTransactionRecord record) {
        return new GroupTransactionDto(record.id(), record.groupId(), record.createdByUserId(),
                groupActivityService.displayName(record.createdByUserId()), record.type(), record.amount(),
                record.category(), record.description(), record.transactionDate(), record.createdAt(),
                record.updatedAt());
    }
}
