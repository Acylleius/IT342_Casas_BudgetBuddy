package edu.casas.budgetbuddy.features.inbox;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inbox")
public class InboxController {
    private final AuthService authService;
    private final InboxService inboxService;

    public InboxController(AuthService authService, InboxService inboxService) {
        this.authService = authService;
        this.inboxService = inboxService;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(inboxService.list(user.id()), "Inbox loaded");
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @PathVariable Long id) {
        UserRecord user = authService.requireUser(authorization);
        inboxService.markRead(user.id(), id);
        return ApiResponse.success(null, "Inbox item marked as read");
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        inboxService.markAllRead(user.id());
        return ApiResponse.success(null, "Inbox marked as read");
    }
}
