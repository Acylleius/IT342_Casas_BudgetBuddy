package edu.casas.budgetbuddy.features.invitations;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.invitations.InvitationsDtos.InvitationRequest;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InvitationsController {
    private final AuthService authService;
    private final InvitationsService invitationsService;

    public InvitationsController(AuthService authService, InvitationsService invitationsService) {
        this.authService = authService;
        this.invitationsService = invitationsService;
    }

    @PostMapping("/api/v1/groups/{groupId}/invitations")
    public ResponseEntity<ApiResponse<InvitationsDtos.InvitationDto>> invite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long groupId,
            @Valid @RequestBody InvitationRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(
                invitationsService.invite(user.id(), groupId, request.email()), "Invite sent successfully"));
    }

    @GetMapping("/api/v1/invitations")
    public ApiResponse<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(invitationsService.listForUser(user.id()), "Invitations loaded");
    }

    @PostMapping("/api/v1/invitations/{invitationId}/accept")
    public ApiResponse<InvitationsDtos.InvitationDto> accept(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long invitationId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(invitationsService.accept(user.id(), invitationId), "Invitation accepted");
    }

    @PostMapping("/api/v1/invitations/{invitationId}/decline")
    public ApiResponse<InvitationsDtos.InvitationDto> decline(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long invitationId) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(invitationsService.decline(user.id(), invitationId), "Invitation declined");
    }
}
