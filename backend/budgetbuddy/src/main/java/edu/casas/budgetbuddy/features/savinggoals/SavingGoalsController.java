package edu.casas.budgetbuddy.features.savinggoals;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.savinggoals.SavingGoalsDtos.ContributionRequest;
import edu.casas.budgetbuddy.features.savinggoals.SavingGoalsDtos.SavingGoalRequest;
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
public class SavingGoalsController {
    private final AuthService authService;
    private final SavingGoalsService savingGoalsService;

    public SavingGoalsController(AuthService authService, SavingGoalsService savingGoalsService) {
        this.authService = authService;
        this.savingGoalsService = savingGoalsService;
    }

    @PostMapping("/api/v1/saving-goals")
    public ResponseEntity<ApiResponse<SavingGoalsDtos.SavingGoalDto>> createPersonal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody SavingGoalRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(savingGoalsService.createPersonal(user.id(), request),
                "Saving goal created"));
    }

    @GetMapping("/api/v1/saving-goals")
    public ApiResponse<?> listPersonal(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(savingGoalsService.listPersonal(user.id()), "Saving goals loaded");
    }

    @PutMapping("/api/v1/saving-goals/{goalId}")
    public ApiResponse<SavingGoalsDtos.SavingGoalDto> updatePersonal(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long goalId,
            @Valid @RequestBody SavingGoalRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(savingGoalsService.updatePersonal(user.id(), goalId, request),
                "Saving goal updated");
    }

    @DeleteMapping("/api/v1/saving-goals/{goalId}")
    public ApiResponse<Void> deletePersonal(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @PathVariable Long goalId) {
        UserRecord user = authService.requireUser(authorization);
        savingGoalsService.deletePersonal(user.id(), goalId);
        return ApiResponse.success(null, "Saving goal deleted");
    }

    @PostMapping("/api/v1/groups/{groupId}/saving-goals")
    public ResponseEntity<ApiResponse<SavingGoalsDtos.SavingGoalDto>> createGroup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @Valid @RequestBody SavingGoalRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(savingGoalsService.createGroup(user.id(), groupId, request),
                "Group saving goal created"));
    }

    @GetMapping("/api/v1/groups/{groupId}/saving-goals")
    public ApiResponse<?> listGroup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long groupId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(savingGoalsService.listGroup(user.id(), groupId), "Group saving goals loaded");
    }

    @PutMapping("/api/v1/groups/{groupId}/saving-goals/{goalId}")
    public ApiResponse<SavingGoalsDtos.SavingGoalDto> updateGroup(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @PathVariable Long goalId,
            @Valid @RequestBody SavingGoalRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(savingGoalsService.updateGroup(user.id(), groupId, goalId, request),
                "Group saving goal updated");
    }

    @DeleteMapping("/api/v1/groups/{groupId}/saving-goals/{goalId}")
    public ApiResponse<Void> deleteGroup(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @PathVariable Long groupId,
                                         @PathVariable Long goalId) {
        UserRecord user = authService.requireUser(authorization);
        savingGoalsService.deleteGroup(user.id(), groupId, goalId);
        return ApiResponse.success(null, "Group saving goal deleted");
    }

    @PostMapping("/api/v1/groups/{groupId}/saving-goals/{goalId}/contribute")
    public ApiResponse<SavingGoalsDtos.SavingGoalDto> contribute(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @PathVariable Long goalId,
            @Valid @RequestBody ContributionRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(savingGoalsService.contribute(user.id(), groupId, goalId, request),
                "Contribution added");
    }
}
