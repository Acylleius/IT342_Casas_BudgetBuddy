package edu.casas.budgetbuddy.features.budgets;

import edu.casas.budgetbuddy.features.budgets.BudgetDtos.BudgetDto;
import edu.casas.budgetbuddy.features.budgets.BudgetDtos.BudgetRequest;
import edu.casas.budgetbuddy.features.budgets.BudgetDtos.BudgetTrackingDto;
import edu.casas.budgetbuddy.features.budgets.BudgetDtos.RelatedTransactionDto;
import edu.casas.budgetbuddy.features.budgets.BudgetDtos.SpenderDto;
import edu.casas.budgetbuddy.features.groups.GroupsService;
import edu.casas.budgetbuddy.features.inbox.InboxService;
import edu.casas.budgetbuddy.shared.persistence.DatabasePersistenceService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.BudgetAlertRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.BudgetRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.GroupTransactionRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.SharedExpenseRecord;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.TransactionRecord;
import edu.casas.budgetbuddy.shared.utils.CategoryUtils;
import edu.casas.budgetbuddy.shared.utils.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BudgetsService {
    private final BudgetBuddyStore store;
    private final GroupsService groupsService;
    private final InboxService inboxService;
    private final DatabasePersistenceService databasePersistenceService;
    private final NumberFormat pesoFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-PH"));

    public BudgetsService(BudgetBuddyStore store, GroupsService groupsService, InboxService inboxService,
                          DatabasePersistenceService databasePersistenceService) {
        this.store = store;
        this.groupsService = groupsService;
        this.inboxService = inboxService;
        this.databasePersistenceService = databasePersistenceService;
    }

    public synchronized BudgetDto createPersonal(Long userId, BudgetRequest request) {
        BudgetRecord record = new BudgetRecord(store.budgetIds.getAndIncrement(), "PERSONAL", userId, null, userId,
                request.name(), request.limitAmount(), normalizePeriod(request.period()), CategoryUtils.optional(request.category()),
                request.startDate(), request.endDate(), false, LocalDateTime.now(), LocalDateTime.now());
        store.budgets.add(record);
        databasePersistenceService.saveBudget(record);
        evaluateBudget(record);
        return toDto(record);
    }

    public List<BudgetDto> listPersonal(Long userId) {
        return store.budgets.stream()
                .filter(budget -> !budget.deleted() && "PERSONAL".equals(budget.scope()) && budget.userId().equals(userId))
                .map(this::toDto)
                .toList();
    }

    public List<BudgetTrackingDto> personalTracking(Long userId) {
        return store.budgets.stream()
                .filter(budget -> !budget.deleted() && "PERSONAL".equals(budget.scope()) && budget.userId().equals(userId))
                .map(this::toTracking)
                .toList();
    }

    public BudgetDto detailPersonal(Long userId, Long budgetId) {
        BudgetRecord budget = requireBudget(budgetId);
        if (!"PERSONAL".equals(budget.scope()) || !budget.userId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Budget access denied");
        }
        return toDto(budget);
    }

    public synchronized BudgetDto updatePersonal(Long userId, Long budgetId, BudgetRequest request) {
        BudgetRecord current = requireBudget(budgetId);
        if (!"PERSONAL".equals(current.scope()) || !current.userId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Budget access denied");
        }
        BudgetRecord replacement = replaceBudget(current, request);
        evaluateBudget(replacement);
        return toDto(replacement);
    }

    public synchronized void deletePersonal(Long userId, Long budgetId) {
        BudgetRecord current = requireBudget(budgetId);
        if (!"PERSONAL".equals(current.scope()) || !current.userId().equals(userId)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "Budget access denied");
        }
        softDelete(current);
    }

    public synchronized BudgetDto createGroup(Long userId, Long groupId, BudgetRequest request) {
        groupsService.requireMember(groupId, userId);
        BudgetRecord record = new BudgetRecord(store.budgetIds.getAndIncrement(), "GROUP", null, groupId, userId,
                request.name(), request.limitAmount(), normalizePeriod(request.period()), CategoryUtils.optional(request.category()),
                request.startDate(), request.endDate(), false, LocalDateTime.now(), LocalDateTime.now());
        store.budgets.add(record);
        databasePersistenceService.saveBudget(record);
        evaluateBudget(record);
        return toDto(record);
    }

    public List<BudgetDto> listGroup(Long userId, Long groupId) {
        groupsService.requireMember(groupId, userId);
        return store.budgets.stream()
                .filter(budget -> !budget.deleted() && "GROUP".equals(budget.scope()) && budget.groupId().equals(groupId))
                .map(this::toDto)
                .toList();
    }

    public List<BudgetTrackingDto> groupTracking(Long userId, Long groupId) {
        groupsService.requireMember(groupId, userId);
        return store.budgets.stream()
                .filter(budget -> !budget.deleted() && "GROUP".equals(budget.scope()) && budget.groupId().equals(groupId))
                .map(this::toTracking)
                .toList();
    }

    public BudgetDto detailGroup(Long userId, Long groupId, Long budgetId) {
        groupsService.requireMember(groupId, userId);
        BudgetRecord budget = requireBudget(budgetId);
        if (!"GROUP".equals(budget.scope()) || !budget.groupId().equals(groupId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Budget not found");
        }
        return toDto(budget);
    }

    public synchronized BudgetDto updateGroup(Long userId, Long groupId, Long budgetId, BudgetRequest request) {
        groupsService.requireMember(groupId, userId);
        BudgetRecord current = requireBudget(budgetId);
        if (!"GROUP".equals(current.scope()) || !current.groupId().equals(groupId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Budget not found");
        }
        BudgetRecord replacement = replaceBudget(current, request);
        evaluateBudget(replacement);
        return toDto(replacement);
    }

    public synchronized void deleteGroup(Long userId, Long groupId, Long budgetId) {
        groupsService.requireMember(groupId, userId);
        BudgetRecord current = requireBudget(budgetId);
        if (!"GROUP".equals(current.scope()) || !current.groupId().equals(groupId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "Budget not found");
        }
        softDelete(current);
    }

    public void evaluatePersonalBudgets(Long userId) {
        store.budgets.stream()
                .filter(budget -> !budget.deleted() && "PERSONAL".equals(budget.scope()) && budget.userId().equals(userId))
                .forEach(this::evaluateBudget);
    }

    public void evaluateGroupBudgets(Long groupId) {
        store.budgets.stream()
                .filter(budget -> !budget.deleted() && "GROUP".equals(budget.scope()) && budget.groupId().equals(groupId))
                .forEach(this::evaluateBudget);
    }

    private void evaluateBudget(BudgetRecord budget) {
        BudgetDto dto = toDto(budget);
        if ("SAFE".equals(dto.status())) {
            return;
        }
        PeriodWindow window = window(budget);
        String alertType = "EXCEEDED".equals(dto.status()) ? "EXCEEDED" : "WARNING";
        if (alertAlreadySent(budget.id(), alertType, window)) {
            return;
        }
        BudgetAlertRecord alert = new BudgetAlertRecord(store.budgetAlertIds.getAndIncrement(), budget.id(),
                alertType, window.start(), window.end(), LocalDateTime.now());
        store.budgetAlerts.add(alert);
        databasePersistenceService.saveBudgetAlert(alert);
        if ("PERSONAL".equals(budget.scope())) {
            String title = "EXCEEDED".equals(alertType) ? "Budget Exceeded" : "Budget Warning";
            String message = "You have used " + dto.percentageUsed().setScale(0, RoundingMode.HALF_UP)
                    + "% of your " + budget.name() + ".";
            inboxService.create(budget.userId(), null, null, "BUDGET_" + alertType, title, message);
            return;
        }
        String groupName = store.groups.stream()
                .filter(group -> group.id().equals(budget.groupId()))
                .map(BudgetBuddyStore.GroupRecord::name)
                .findFirst()
                .orElse("Group");
        String title = "EXCEEDED".equals(alertType) ? "Group Budget Exceeded" : "Group Budget Warning";
        String message = groupName + ("EXCEEDED".equals(alertType) ? " has exceeded " : " is near ")
                + budget.name() + ". Current spending is " + pesoFormat.format(dto.spentAmount())
                + " out of " + pesoFormat.format(dto.limitAmount()) + ".";
        store.members.stream()
                .filter(member -> member.groupId().equals(budget.groupId()) && !member.deleted())
                .forEach(member -> inboxService.create(member.userId(), budget.groupId(), null,
                        "GROUP_BUDGET_" + alertType, title, message));
    }

    private boolean alertAlreadySent(Long budgetId, String alertType, PeriodWindow window) {
        return store.budgetAlerts.stream()
                .anyMatch(alert -> alert.budgetId().equals(budgetId)
                        && alert.alertType().equals(alertType)
                        && alert.periodStart().equals(window.start())
                        && alert.periodEnd().equals(window.end()));
    }

    private BudgetTrackingDto toTracking(BudgetRecord budget) {
        return new BudgetTrackingDto(toDto(budget), relatedTransactions(budget), contributors(budget), LocalDateTime.now());
    }

    private BudgetDto toDto(BudgetRecord budget) {
        BigDecimal spent = spentAmount(budget);
        BigDecimal remaining = budget.limitAmount().subtract(spent);
        BigDecimal percentage = budget.limitAmount().compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : spent.multiply(BigDecimal.valueOf(100)).divide(budget.limitAmount(), 2, RoundingMode.HALF_UP);
        String status = percentage.compareTo(BigDecimal.valueOf(100)) > 0 ? "EXCEEDED"
                : percentage.compareTo(BigDecimal.valueOf(80)) >= 0 ? "WARNING" : "SAFE";
        return new BudgetDto(budget.id(), budget.scope(), budget.userId(), budget.groupId(), budget.createdByUserId(),
                budget.name(), budget.limitAmount(), budget.period(), budget.category(), effectiveStart(budget),
                effectiveEnd(budget), spent, remaining, percentage, status, budget.createdAt(), budget.updatedAt());
    }

    private BigDecimal spentAmount(BudgetRecord budget) {
        return relatedTransactions(budget).stream()
                .map(RelatedTransactionDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RelatedTransactionDto> relatedTransactions(BudgetRecord budget) {
        PeriodWindow window = window(budget);
        if ("PERSONAL".equals(budget.scope())) {
            return store.transactions.stream()
                    .filter(transaction -> transaction.userId().equals(budget.userId()) && !transaction.deleted())
                    .filter(transaction -> "EXPENSE".equals(transaction.type()))
                    .filter(transaction -> within(transaction.transactionDate(), window))
                    .filter(transaction -> categoryMatches(budget.category(), transaction.category()))
                    .map(transaction -> new RelatedTransactionDto(transaction.id(), transaction.userId(), null,
                            transaction.type(), transaction.amount(), transaction.category(), transaction.description(),
                            transaction.transactionDate()))
                    .toList();
        }
        List<Long> linkedExpenseIds = store.groupTransactions.stream()
                .filter(transaction -> transaction.sharedExpenseId() != null)
                .map(GroupTransactionRecord::sharedExpenseId)
                .toList();
        List<RelatedTransactionDto> groupTransactions = store.groupTransactions.stream()
                .filter(transaction -> transaction.groupId().equals(budget.groupId()) && !transaction.deleted())
                .filter(transaction -> "APPROVED".equals(transaction.verificationStatus()))
                .filter(transaction -> "EXPENSE".equals(transaction.type()))
                .filter(transaction -> within(transaction.transactionDate(), window))
                .filter(transaction -> categoryMatches(budget.category(), transaction.category()))
                .map(transaction -> new RelatedTransactionDto(transaction.id(), transaction.createdByUserId(),
                        transaction.groupId(), transaction.type(), transaction.amount(), transaction.category(),
                        transaction.description(), transaction.transactionDate()))
                .toList();
        List<RelatedTransactionDto> directSharedExpenses = store.expenses.stream()
                .filter(expense -> expense.groupId().equals(budget.groupId()) && !expense.deleted())
                .filter(expense -> !linkedExpenseIds.contains(expense.id()))
                .filter(expense -> within(expense.expenseDate(), window))
                .filter(expense -> categoryMatches(budget.category(), expense.category()))
                .map(expense -> new RelatedTransactionDto(expense.id(), expense.paidBy(), expense.groupId(),
                        "EXPENSE", expense.amount(), expense.category(), expense.description(), expense.expenseDate()))
                .toList();
        return java.util.stream.Stream.concat(groupTransactions.stream(), directSharedExpenses.stream()).toList();
    }

    private List<SpenderDto> contributors(BudgetRecord budget) {
        if (!"GROUP".equals(budget.scope())) {
            return List.of();
        }
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        relatedTransactions(budget).forEach(transaction ->
                totals.merge(transaction.userId(), transaction.amount(), BigDecimal::add));
        return totals.entrySet().stream()
                .map(entry -> new SpenderDto(entry.getKey(), displayName(entry.getKey()), entry.getValue()))
                .toList();
    }

    private BudgetRecord requireBudget(Long budgetId) {
        return store.budgets.stream()
                .filter(budget -> budget.id().equals(budgetId) && !budget.deleted())
                .findFirst()
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private BudgetRecord replaceBudget(BudgetRecord current, BudgetRequest request) {
        BudgetRecord replacement = new BudgetRecord(current.id(), current.scope(), current.userId(), current.groupId(),
                current.createdByUserId(), request.name(), request.limitAmount(), normalizePeriod(request.period()),
                CategoryUtils.optional(request.category()), request.startDate(), request.endDate(), false,
                current.createdAt(), LocalDateTime.now());
        replace(replacement);
        databasePersistenceService.saveBudget(replacement);
        return replacement;
    }

    private void softDelete(BudgetRecord current) {
        BudgetRecord replacement = new BudgetRecord(current.id(), current.scope(), current.userId(), current.groupId(),
                current.createdByUserId(), current.name(), current.limitAmount(), current.period(), current.category(),
                current.startDate(), current.endDate(), true, current.createdAt(), LocalDateTime.now());
        replace(replacement);
        databasePersistenceService.saveBudget(replacement);
    }

    private void replace(BudgetRecord replacement) {
        for (int index = 0; index < store.budgets.size(); index++) {
            if (store.budgets.get(index).id().equals(replacement.id())) {
                store.budgets.set(index, replacement);
                return;
            }
        }
    }

    private String normalizePeriod(String period) {
        String normalized = period == null ? "" : period.trim().toUpperCase();
        if (!normalized.equals("WEEKLY") && !normalized.equals("MONTHLY")) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "period must be WEEKLY or MONTHLY");
        }
        return normalized;
    }

    private boolean categoryMatches(String budgetCategory, String transactionCategory) {
        return CategoryUtils.matches(budgetCategory, transactionCategory);
    }

    private boolean within(LocalDate date, PeriodWindow window) {
        return !date.isBefore(window.start()) && !date.isAfter(window.end());
    }

    private LocalDate effectiveStart(BudgetRecord budget) {
        return window(budget).start();
    }

    private LocalDate effectiveEnd(BudgetRecord budget) {
        return window(budget).end();
    }

    private PeriodWindow window(BudgetRecord budget) {
        if (budget.startDate() != null && budget.endDate() != null) {
            return new PeriodWindow(budget.startDate(), budget.endDate());
        }
        LocalDate today = LocalDate.now();
        if ("WEEKLY".equals(budget.period())) {
            LocalDate start = budget.startDate() == null
                    ? today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    : budget.startDate();
            LocalDate end = budget.endDate() == null ? start.plusDays(6) : budget.endDate();
            return new PeriodWindow(start, end);
        }
        LocalDate start = budget.startDate() == null ? today.withDayOfMonth(1) : budget.startDate();
        LocalDate end = budget.endDate() == null ? start.withDayOfMonth(start.lengthOfMonth()) : budget.endDate();
        return new PeriodWindow(start, end);
    }

    private String displayName(Long userId) {
        return store.users.stream()
                .filter(user -> user.id().equals(userId))
                .map(user -> (user.firstname() + " " + user.lastname()).trim())
                .findFirst()
                .orElse("User #" + userId);
    }

    private record PeriodWindow(LocalDate start, LocalDate end) {
    }
}
