package edu.casas.budgetbuddy.features.grouptransactions;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.grouptransactions.GroupTransactionsDtos.GroupTransactionRequest;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupTransactionsController {
    private final AuthService authService;
    private final GroupTransactionsService groupTransactionsService;

    public GroupTransactionsController(AuthService authService, GroupTransactionsService groupTransactionsService) {
        this.authService = authService;
        this.groupTransactionsService = groupTransactionsService;
    }

    @PostMapping("/api/v1/groups/{groupId}/transactions")
    public ResponseEntity<ApiResponse<GroupTransactionsDtos.GroupTransactionDto>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupTransactionRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(groupTransactionsService.create(user.id(), groupId,
                request.type(), request.amount(), request.category(), request.description(),
                request.actorUserId(), request.transactionDate()), "Group transaction saved"));
    }

    @GetMapping("/api/v1/groups/{groupId}/transactions")
    public ApiResponse<?> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupTransactionsService.list(user.id(), groupId), "Group transactions loaded");
    }

    @GetMapping("/api/v1/groups/{groupId}/transactions/summary")
    public ApiResponse<GroupTransactionsDtos.GroupSummaryDto> summary(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupTransactionsService.summary(user.id(), groupId), "Group summary loaded");
    }

    @PutMapping("/api/v1/groups/{groupId}/transactions/{transactionId}")
    public ApiResponse<GroupTransactionsDtos.GroupTransactionDto> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @PathVariable Long transactionId,
            @Valid @RequestBody GroupTransactionRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(groupTransactionsService.update(user.id(), groupId, transactionId,
                request.type(), request.amount(), request.category(), request.description(),
                request.transactionDate()), "Group transaction updated");
    }

    @DeleteMapping("/api/v1/groups/{groupId}/transactions/{transactionId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long groupId,
                                    @PathVariable Long transactionId) {
        UserRecord user = authService.requireUser(authorization);
        groupTransactionsService.delete(user.id(), groupId, transactionId);
        return ApiResponse.success(null, "Group transaction deleted");
    }
}
