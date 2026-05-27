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
import edu.casas.budgetbuddy.shared.utils.CategoryUtils;
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
        String normalizedCategory = CategoryUtils.require(category);
        LocalDate actualDate = transactionDate == null ? LocalDate.now() : transactionDate;
        String verificationStatus = actor.equals(requesterId) ? "APPROVED" : "PENDING_VERIFICATION";
        Long sharedExpenseId = null;
        if ("EXPENSE".equals(normalizedType) && "APPROVED".equals(verificationStatus)) {
            sharedExpenseId = createBalanceExpense(groupId, actor, amount, normalizedCategory, description, actualDate);
        }
        GroupTransactionRecord record = new GroupTransactionRecord(store.groupTransactionIds.getAndIncrement(),
                groupId, requesterId, normalizedType, amount, normalizedCategory, description, actualDate,
                LocalDateTime.now(), LocalDateTime.now(), sharedExpenseId, false, actor, verificationStatus,
                "APPROVED".equals(verificationStatus) ? actor : null,
                "APPROVED".equals(verificationStatus) ? LocalDateTime.now() : null, null);
        store.groupTransactions.add(record);
        databasePersistenceService.saveGroupTransaction(record);

        String action = "INCOME".equals(normalizedType) ? "GROUP_INCOME_CREATED" : "GROUP_EXPENSE_CREATED";
        String noun = "INCOME".equals(normalizedType) ? "Income" : "Expense";
        String descriptionText;
        if ("PENDING_VERIFICATION".equals(verificationStatus)) {
            descriptionText = groupActivityService.displayName(requesterId) + " created a pending "
                    + noun.toLowerCase() + " for " + groupActivityService.displayName(actor) + ": "
                    + normalizedCategory + " " + pesoFormat.format(amount);
            groupActivityService.log(groupId, requesterId, "GROUP_TRANSACTION_PENDING", "GROUP_TRANSACTION",
                    record.id(), null, "PENDING_VERIFICATION", descriptionText);
            inboxService.createAction(actor, groupId, "GROUP_TRANSACTION", record.id(),
                    "TRANSACTION_VERIFICATION", "Transaction Verification Needed",
                    groupActivityService.displayName(requesterId) + " made a transaction with your name for "
                            + normalizedCategory + " amounting to " + pesoFormat.format(amount)
                            + ". Please verify if this transaction is valid.");
        } else {
            descriptionText = groupActivityService.displayName(actor) + " added " + noun + " "
                    + normalizedCategory + " " + pesoFormat.format(amount);
            groupActivityService.log(groupId, actor, action, "GROUP_TRANSACTION", record.id(),
                    null, pesoFormat.format(amount), descriptionText);
            notifyGroup(groupId, actor, "Group " + noun.toLowerCase() + " added", descriptionText,
                    "INCOME".equals(normalizedType) ? "GROUP_INCOME_CREATED" : "GROUP_EXPENSE_CREATED");
            budgetsService.evaluateGroupBudgets(groupId);
        }
        GroupTransactionDto dto = toDto(record);
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
                .filter(record -> record.groupId().equals(groupId) && !record.deleted()
                        && "APPROVED".equals(record.verificationStatus()))
                .toList();
        BigDecimal income = sum(records, "INCOME");
        BigDecimal expenses = sum(records, "EXPENSE");
        return new GroupSummaryDto(income, expenses, income.subtract(expenses));
    }

    public synchronized GroupTransactionDto update(Long requesterId, Long groupId, Long transactionId,
                                                   String type, BigDecimal amount, String category,
                                                   String description, Long actorUserId,
                                                   LocalDate transactionDate) {
        groupsService.requireMember(groupId, requesterId);
        String normalizedType = normalizeType(type);
        String normalizedCategory = CategoryUtils.require(category);
        for (int index = 0; index < store.groupTransactions.size(); index++) {
            GroupTransactionRecord current = store.groupTransactions.get(index);
            if (current.id().equals(transactionId) && current.groupId().equals(groupId) && !current.deleted()) {
                if (!current.createdByUserId().equals(requesterId)) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "Only the creator can update this group transaction");
                }
                if ("DECLINED".equals(current.verificationStatus())) {
                    throw new DomainException(HttpStatus.CONFLICT, "Declined transactions cannot be updated");
                }
                Long selectedUserId = actorUserId == null ? current.selectedUserId() : actorUserId;
                groupsService.requireMember(groupId, selectedUserId);
                String verificationStatus = selectedUserId.equals(requesterId) ? "APPROVED" : "PENDING_VERIFICATION";
                Long sharedExpenseId = current.sharedExpenseId();
                LocalDate actualDate = transactionDate == null ? current.transactionDate() : transactionDate;
                if (!"APPROVED".equals(verificationStatus) || !"EXPENSE".equals(normalizedType)) {
                    softDeleteLinkedExpense(sharedExpenseId);
                    sharedExpenseId = null;
                } else if (sharedExpenseId == null) {
                    sharedExpenseId = createBalanceExpense(groupId, selectedUserId, amount, normalizedCategory,
                            description, actualDate);
                }
                GroupTransactionRecord replacement = new GroupTransactionRecord(current.id(), current.groupId(),
                        current.createdByUserId(), normalizedType, amount, normalizedCategory, description,
                        actualDate, current.createdAt(), LocalDateTime.now(), sharedExpenseId, false, selectedUserId,
                        verificationStatus, "APPROVED".equals(verificationStatus) ? selectedUserId : null,
                        "APPROVED".equals(verificationStatus) ? LocalDateTime.now() : null, null);
                store.groupTransactions.set(index, replacement);
                databasePersistenceService.saveGroupTransaction(replacement);
                updateLinkedExpense(sharedExpenseId, amount, normalizedCategory, description, replacement.transactionDate());
                String oldValue = current.category() + " " + pesoFormat.format(current.amount());
                String newValue = normalizedCategory + " " + pesoFormat.format(amount);
                String descriptionText = groupActivityService.displayName(requesterId) + " updated "
                        + normalizedCategory + " from " + pesoFormat.format(current.amount()) + " to " + pesoFormat.format(amount);
                groupActivityService.log(groupId, requesterId,
                        "PENDING_VERIFICATION".equals(verificationStatus) ? "GROUP_TRANSACTION_PENDING"
                                : "INCOME".equals(normalizedType) ? "GROUP_INCOME_UPDATED" : "GROUP_EXPENSE_UPDATED",
                        "GROUP_TRANSACTION", transactionId, oldValue, newValue, descriptionText);
                if ("PENDING_VERIFICATION".equals(verificationStatus)) {
                    inboxService.createAction(selectedUserId, groupId, "GROUP_TRANSACTION", current.id(),
                            "TRANSACTION_VERIFICATION", "Transaction Verification Needed",
                            groupActivityService.displayName(requesterId) + " made a transaction with your name for "
                                    + normalizedCategory + " amounting to " + pesoFormat.format(amount)
                                    + ". Please verify if this transaction is valid.");
                } else {
                    notifyGroup(groupId, requesterId, "Group transaction updated", descriptionText, "GROUP_UPDATE");
                    budgetsService.evaluateGroupBudgets(groupId);
                }
                GroupTransactionDto dto = toDto(replacement);
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
                        LocalDateTime.now(), current.sharedExpenseId(), true, current.selectedUserId(),
                        current.verificationStatus(), current.verifiedByUserId(), current.verifiedAt(),
                        current.declinedAt()));
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

    public synchronized GroupTransactionDto verify(Long requesterId, Long groupId, Long transactionId, String decision) {
        groupsService.requireMember(groupId, requesterId);
        String normalizedDecision = decision == null ? "" : decision.trim().toUpperCase();
        if (!normalizedDecision.equals("ACCEPT") && !normalizedDecision.equals("DECLINE")) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "decision must be ACCEPT or DECLINE");
        }
        for (int index = 0; index < store.groupTransactions.size(); index++) {
            GroupTransactionRecord current = store.groupTransactions.get(index);
            if (current.id().equals(transactionId) && current.groupId().equals(groupId) && !current.deleted()) {
                if (!current.selectedUserId().equals(requesterId)) {
                    throw new DomainException(HttpStatus.FORBIDDEN, "Only the selected user can verify this transaction");
                }
                if (!"PENDING_VERIFICATION".equals(current.verificationStatus())) {
                    throw new DomainException(HttpStatus.CONFLICT, "Transaction verification is already resolved");
                }
                GroupTransactionRecord replacement;
                if ("ACCEPT".equals(normalizedDecision)) {
                    Long sharedExpenseId = current.sharedExpenseId();
                    if ("EXPENSE".equals(current.type()) && sharedExpenseId == null) {
                        sharedExpenseId = createBalanceExpense(groupId, current.selectedUserId(), current.amount(),
                                current.category(), current.description(), current.transactionDate());
                    }
                    replacement = new GroupTransactionRecord(current.id(), current.groupId(), current.createdByUserId(),
                            current.type(), current.amount(), current.category(), current.description(),
                            current.transactionDate(), current.createdAt(), LocalDateTime.now(), sharedExpenseId,
                            false, current.selectedUserId(), "APPROVED", requesterId, LocalDateTime.now(), null);
                    store.groupTransactions.set(index, replacement);
                    databasePersistenceService.saveGroupTransaction(replacement);
                    String descriptionText = groupActivityService.displayName(requesterId)
                            + " accepted the verification for " + current.category() + " "
                            + pesoFormat.format(current.amount());
                    groupActivityService.log(groupId, requesterId, "GROUP_TRANSACTION_VERIFIED",
                            "GROUP_TRANSACTION", current.id(), "PENDING_VERIFICATION", "APPROVED", descriptionText);
                    inboxService.resolveAction(requesterId, "GROUP_TRANSACTION", current.id(), "ACCEPTED");
                    notifyGroup(groupId, requesterId, "Group transaction verified", descriptionText, "GROUP_UPDATE");
                    budgetsService.evaluateGroupBudgets(groupId);
                } else {
                    replacement = new GroupTransactionRecord(current.id(), current.groupId(), current.createdByUserId(),
                            current.type(), current.amount(), current.category(), current.description(),
                            current.transactionDate(), current.createdAt(), LocalDateTime.now(), current.sharedExpenseId(),
                            false, current.selectedUserId(), "DECLINED", null, null, LocalDateTime.now());
                    store.groupTransactions.set(index, replacement);
                    databasePersistenceService.saveGroupTransaction(replacement);
                    softDeleteLinkedExpense(current.sharedExpenseId());
                    String descriptionText = groupActivityService.displayName(requesterId)
                            + " declined the verification for " + current.category() + " "
                            + pesoFormat.format(current.amount());
                    groupActivityService.log(groupId, requesterId, "GROUP_TRANSACTION_DECLINED",
                            "GROUP_TRANSACTION", current.id(), "PENDING_VERIFICATION", "DECLINED", descriptionText);
                    inboxService.resolveAction(requesterId, "GROUP_TRANSACTION", current.id(), "DECLINED");
                    inboxService.create(current.createdByUserId(), groupId, null, "GROUP_TRANSACTION_DECLINED",
                            "Transaction Verification Declined", descriptionText);
                    budgetsService.evaluateGroupBudgets(groupId);
                }
                realtimeService.publishToUsers(activeMemberIds(groupId), "group-transaction-updated", toDto(replacement));
                return toDto(replacement);
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
                .filter(record -> "APPROVED".equals(record.verificationStatus()))
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
        return new GroupTransactionDto(record.id(), record.groupId(), record.selectedUserId(),
                groupActivityService.displayName(record.selectedUserId()), record.type(), record.amount(),
                record.category(), record.description(), record.transactionDate(), record.createdAt(),
                record.updatedAt(), record.createdByUserId(), record.verificationStatus(),
                record.verifiedByUserId(), record.verifiedAt(), record.declinedAt());
    }
}
