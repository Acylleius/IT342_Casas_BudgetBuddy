package edu.casas.budgetbuddy.shared.store;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class BudgetBuddyStore {
    public final AtomicLong userIds = new AtomicLong(1);
    public final AtomicLong transactionIds = new AtomicLong(1);
    public final AtomicLong groupIds = new AtomicLong(1);
    public final AtomicLong expenseIds = new AtomicLong(1);
    public final AtomicLong splitIds = new AtomicLong(1);
    public final AtomicLong activityIds = new AtomicLong(1);
    public final AtomicLong notificationIds = new AtomicLong(1);
    public final AtomicLong groupActivityIds = new AtomicLong(1);
    public final AtomicLong invitationIds = new AtomicLong(1);
    public final AtomicLong inboxIds = new AtomicLong(1);
    public final AtomicLong groupTransactionIds = new AtomicLong(1);
    public final AtomicLong budgetIds = new AtomicLong(1);
    public final AtomicLong budgetAlertIds = new AtomicLong(1);
    public final AtomicLong savingGoalIds = new AtomicLong(1);
    public final AtomicLong savingGoalContributionIds = new AtomicLong(1);
    public final List<UserRecord> users = new ArrayList<>();
    public final List<TransactionRecord> transactions = new ArrayList<>();
    public final List<GroupRecord> groups = new ArrayList<>();
    public final List<GroupMemberRecord> members = new ArrayList<>();
    public final List<SharedExpenseRecord> expenses = new ArrayList<>();
    public final List<ExpenseSplitRecord> splits = new ArrayList<>();
    public final List<ActivityLogRecord> activityLogs = new ArrayList<>();
    public final List<EmailNotificationRecord> emailNotifications = new ArrayList<>();
    public final List<GroupActivityLogRecord> groupActivityLogs = new ArrayList<>();
    public final List<GroupInvitationRecord> groupInvitations = new ArrayList<>();
    public final List<InboxNotificationRecord> inboxNotifications = new ArrayList<>();
    public final List<GroupTransactionRecord> groupTransactions = new ArrayList<>();
    public final List<BudgetRecord> budgets = new ArrayList<>();
    public final List<BudgetAlertRecord> budgetAlerts = new ArrayList<>();
    public final List<SavingGoalRecord> savingGoals = new ArrayList<>();
    public final List<SavingGoalContributionRecord> savingGoalContributions = new ArrayList<>();

    public synchronized void reset() {
        userIds.set(1);
        transactionIds.set(1);
        groupIds.set(1);
        expenseIds.set(1);
        splitIds.set(1);
        activityIds.set(1);
        notificationIds.set(1);
        groupActivityIds.set(1);
        invitationIds.set(1);
        inboxIds.set(1);
        groupTransactionIds.set(1);
        budgetIds.set(1);
        budgetAlertIds.set(1);
        savingGoalIds.set(1);
        savingGoalContributionIds.set(1);
        users.clear();
        transactions.clear();
        groups.clear();
        members.clear();
        expenses.clear();
        splits.clear();
        activityLogs.clear();
        emailNotifications.clear();
        groupActivityLogs.clear();
        groupInvitations.clear();
        inboxNotifications.clear();
        groupTransactions.clear();
        budgets.clear();
        budgetAlerts.clear();
        savingGoals.clear();
        savingGoalContributions.clear();
    }

    public record UserRecord(Long id, String email, String passwordHash, String firstname,
                             String lastname, String role, String authProvider, String googleId,
                             LocalDateTime createdAt) {
    }

    public record TransactionRecord(Long id, Long userId, String type, BigDecimal amount,
                                    String category, String description, LocalDate transactionDate,
                                    boolean deleted) {
    }

    public record GroupRecord(Long id, String name, String description, Long createdBy,
                              boolean deleted) {
    }

    public record GroupMemberRecord(Long groupId, Long userId, String role, boolean deleted) {
    }

    public record SharedExpenseRecord(Long id, Long groupId, Long paidBy, BigDecimal amount,
                                      String category, String description, LocalDate expenseDate,
                                      boolean deleted) {
    }

    public record ExpenseSplitRecord(Long id, Long expenseId, Long userId, BigDecimal amount,
                                     boolean settled, LocalDateTime settledAt, boolean deleted) {
    }

    public record ActivityLogRecord(Long id, Long userId, String action, String entityType,
                                    Long entityId, String description, LocalDateTime createdAt) {
    }

    public record EmailNotificationRecord(Long id, Long recipientUserId, String recipientEmail,
                                          String subject, String message, boolean sent,
                                          String status, LocalDateTime createdAt) {
    }

    public record GroupActivityLogRecord(Long id, Long groupId, Long actorUserId, String actorUsername,
                                         String actionType, String entityType, Long entityId,
                                         String oldValue, String newValue, String description,
                                         LocalDateTime createdAt) {
    }

    public record GroupInvitationRecord(Long id, Long groupId, Long invitedUserId, Long invitedByUserId,
                                        String status, LocalDateTime createdAt, LocalDateTime respondedAt) {
    }

    public record InboxNotificationRecord(Long id, Long recipientUserId, Long groupId, Long invitationId,
                                          String type, String title, String message, boolean read,
                                          LocalDateTime createdAt) {
    }

    public record GroupTransactionRecord(Long id, Long groupId, Long createdByUserId, String type,
                                         BigDecimal amount, String category, String description,
                                         LocalDate transactionDate, LocalDateTime createdAt,
                                         LocalDateTime updatedAt, Long sharedExpenseId, boolean deleted) {
    }

    public record BudgetRecord(Long id, String scope, Long userId, Long groupId, Long createdByUserId,
                               String name, BigDecimal limitAmount, String period, String category,
                               LocalDate startDate, LocalDate endDate, boolean deleted,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record BudgetAlertRecord(Long id, Long budgetId, String alertType,
                                    LocalDate periodStart, LocalDate periodEnd,
                                    LocalDateTime createdAt) {
    }

    public record SavingGoalRecord(Long id, String scope, Long userId, Long groupId, Long createdByUserId,
                                   String title, BigDecimal targetAmount, BigDecimal currentAmount,
                                   LocalDate deadline, boolean deleted, boolean completionNotified,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record SavingGoalContributionRecord(Long id, Long savingGoalId, Long userId,
                                               BigDecimal amount, String note, LocalDateTime createdAt) {
    }
}
