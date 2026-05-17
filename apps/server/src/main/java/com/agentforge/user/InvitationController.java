package com.agentforge.user;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping
    public ResponseEntity<List<Invitation>> getInvitations(Authentication authentication) {
        Long tenantId = getTenantIdFromAuthentication(authentication);
        return ResponseEntity.ok(invitationService.getInvitations(tenantId));
    }

    @PostMapping
    public ResponseEntity<Invitation> createInvitation(
            @RequestBody CreateInvitationRequest request,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuthentication(authentication);
        Long userId = getUserIdFromAuthentication(authentication);

        Invitation invitation = invitationService.createInvitation(
            tenantId, userId, request.getDescription(), request.getDaysValid()
        );
        return ResponseEntity.ok(invitation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvitation(
            @PathVariable Long id,
            Authentication authentication) {
        Long tenantId = getTenantIdFromAuthentication(authentication);
        invitationService.deleteInvitation(id, tenantId);
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CreateInvitationRequest {
        private String description;
        private int daysValid = 7;
    }

    private Long getTenantIdFromAuthentication(Authentication authentication) {
        return (Long) authentication.getDetails();
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        return Long.parseLong(authentication.getName());
    }
}
