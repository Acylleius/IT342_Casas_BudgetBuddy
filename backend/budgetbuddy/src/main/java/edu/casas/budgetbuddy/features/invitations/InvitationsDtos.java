package edu.casas.budgetbuddy.features.invitations;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public final class InvitationsDtos {
    private InvitationsDtos() {
    }

    public record InvitationRequest(@Email @NotBlank String email) {
    }

    public record InvitationDto(Long id, Long groupId, String groupName, Long invitedUserId,
                                Long invitedByUserId, String invitedByName, String status,
                                LocalDateTime createdAt, LocalDateTime respondedAt) {
    }
}
