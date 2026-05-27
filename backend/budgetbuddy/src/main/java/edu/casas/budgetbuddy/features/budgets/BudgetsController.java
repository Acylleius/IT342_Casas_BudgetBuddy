package edu.casas.budgetbuddy.features.budgets;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.budgets.BudgetDtos.BudgetRequest;
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
public class BudgetsController {
    private final AuthService authService;
    private final BudgetsService budgetsService;

    public BudgetsController(AuthService authService, BudgetsService budgetsService) {
        this.authService = authService;
        this.budgetsService = budgetsService;
    }

    @PostMapping("/api/v1/budgets")
    public ResponseEntity<ApiResponse<BudgetDtos.BudgetDto>> createPersonal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody BudgetRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(budgetsService.createPersonal(user.id(), request),
                "Budget created"));
    }

    @GetMapping("/api/v1/budgets")
    public ApiResponse<?> listPersonal(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.listPersonal(user.id()), "Budgets loaded");
    }

    @GetMapping("/api/v1/budgets/tracking")
    public ApiResponse<?> personalTracking(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.personalTracking(user.id()), "Budget tracking loaded");
    }

    @GetMapping("/api/v1/budgets/{budgetId}")
    public ApiResponse<BudgetDtos.BudgetDto> detailPersonal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long budgetId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.detailPersonal(user.id(), budgetId), "Budget loaded");
    }

    @PutMapping("/api/v1/budgets/{budgetId}")
    public ApiResponse<BudgetDtos.BudgetDto> updatePersonal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.updatePersonal(user.id(), budgetId, request), "Budget updated");
    }

    @DeleteMapping("/api/v1/budgets/{budgetId}")
    public ApiResponse<Void> deletePersonal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long budgetId) {
        UserRecord user = authService.requireUser(authorization);
        budgetsService.deletePersonal(user.id(), budgetId);
        return ApiResponse.success(null, "Budget deleted");
    }

    @PostMapping("/api/v1/groups/{groupId}/budgets")
    public ResponseEntity<ApiResponse<BudgetDtos.BudgetDto>> createGroup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @Valid @RequestBody BudgetRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(budgetsService.createGroup(user.id(), groupId, request),
                "Group budget created"));
    }

    @GetMapping("/api/v1/groups/{groupId}/budgets")
    public ApiResponse<?> listGroup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.listGroup(user.id(), groupId), "Group budgets loaded");
    }

    @GetMapping("/api/v1/groups/{groupId}/budgets/tracking")
    public ApiResponse<?> groupTracking(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.groupTracking(user.id(), groupId), "Group budget tracking loaded");
    }

    @GetMapping("/api/v1/groups/{groupId}/budgets/{budgetId}")
    public ApiResponse<BudgetDtos.BudgetDto> detailGroup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @PathVariable Long budgetId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.detailGroup(user.id(), groupId, budgetId), "Group budget loaded");
    }

    @PutMapping("/api/v1/groups/{groupId}/budgets/{budgetId}")
    public ApiResponse<BudgetDtos.BudgetDto> updateGroup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @PathVariable Long budgetId,
            @Valid @RequestBody BudgetRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(budgetsService.updateGroup(user.id(), groupId, budgetId, request),
                "Group budget updated");
    }

    @DeleteMapping("/api/v1/groups/{groupId}/budgets/{budgetId}")
    public ApiResponse<Void> deleteGroup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable Long groupId,
                                         @PathVariable Long budgetId) {
        UserRecord user = authService.requireUser(authorization);
        budgetsService.deleteGroup(user.id(), groupId, budgetId);
        return ApiResponse.success(null, "Group budget deleted");
    }
}
