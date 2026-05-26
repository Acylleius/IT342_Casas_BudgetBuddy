package edu.casas.budgetbuddy.features.activity;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {
    private final AuthService authService;
    private final ActivityService activityService;

    public ActivityController(AuthService authService, ActivityService activityService) {
        this.authService = authService;
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<?> recent(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestParam(defaultValue = "20") int limit) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(activityService.recentForUser(user.id(), limit), "Activity loaded");
    }
}
