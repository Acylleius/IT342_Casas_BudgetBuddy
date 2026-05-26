package edu.casas.budgetbuddy.features.sharedexpenses;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.sharedexpenses.SharedExpensesDtos.SharedExpenseRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SharedExpensesController {
    private final AuthService authService;
    private final SharedExpensesService sharedExpensesService;

    public SharedExpensesController(AuthService authService, SharedExpensesService sharedExpensesService) {
        this.authService = authService;
        this.sharedExpensesService = sharedExpensesService;
    }

    @PostMapping("/api/v1/groups/{groupId}/shared-expenses")
    public ResponseEntity<ApiResponse<SharedExpensesDtos.SharedExpenseDto>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @Valid @RequestBody SharedExpenseRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(sharedExpensesService.create(user.id(), groupId, request.amount(),
                request.category(), request.description(), request.paidBy(), request.expenseDate(),
                request.participantUserIds()), "Shared expense saved"));
    }

    @PostMapping("/api/v1/sharedexpenses")
    public ResponseEntity<ApiResponse<SharedExpensesDtos.SharedExpenseDto>> createAlias(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SharedExpenseRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(sharedExpensesService.create(user.id(), request.groupId(),
                request.amount(), request.category(), request.description(), request.paidBy(), request.expenseDate(),
                request.participantUserIds()), "Shared expense saved"));
    }

    @GetMapping("/api/v1/groups/{groupId}/shared-expenses")
    public ApiResponse<?> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(sharedExpensesService.list(user.id(), groupId), "Shared expenses loaded");
    }

    @GetMapping("/api/v1/sharedexpenses")
    public ApiResponse<?> listAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(sharedExpensesService.listForUser(user.id()), "Shared expenses loaded");
    }

    @GetMapping("/api/v1/groups/{groupId}/balances")
    public ApiResponse<?> balances(@RequestHeader(value = "Authorization", required = false) String authorization,
                                   @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(sharedExpensesService.balances(user.id(), groupId), "Balances loaded");
    }

    @GetMapping("/api/v1/sharedexpenses/balances")
    public ApiResponse<?> balancesAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(sharedExpensesService.balancesForUser(user.id()), "Balances loaded");
    }

    @PutMapping("/api/v1/shared-expenses/{expenseId}")
    public ApiResponse<SharedExpensesDtos.SharedExpenseDto> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long expenseId,
            @Valid @RequestBody SharedExpenseRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(sharedExpensesService.update(user.id(), expenseId, request.amount(),
                request.category(), request.description(), request.expenseDate()), "Shared expense updated");
    }

    @DeleteMapping("/api/v1/shared-expenses/{expenseId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long expenseId) {
        UserRecord user = authService.requireUser(authorization);
        sharedExpensesService.softDelete(user.id(), expenseId);
        return ApiResponse.success(null, "Shared expense deleted");
    }

    @PostMapping("/api/v1/shared-expenses/splits/{splitId}/settle")
    public ApiResponse<Void> settle(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long splitId) {
        UserRecord user = authService.requireUser(authorization);
        sharedExpensesService.settle(user.id(), splitId);
        return ApiResponse.success(null, "Split settled");
    }
}
