package edu.casas.budgetbuddy.features.realtime;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class RealtimeController {
    private final AuthService authService;
    private final RealtimeService realtimeService;

    public RealtimeController(AuthService authService, RealtimeService realtimeService) {
        this.authService = authService;
        this.realtimeService = realtimeService;
    }

    @GetMapping("/api/v1/realtime/stream")
    public SseEmitter stream(@RequestParam String token) {
        UserRecord user = authService.requireUser("Bearer " + token);
        return realtimeService.subscribe(user.id());
    }
}
