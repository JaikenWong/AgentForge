package com.agentforge.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Invitation> getInvitations(Long tenantId) {
        return invitationRepository.findByTenantId(tenantId);
    }

    public Invitation createInvitation(Long tenantId, Long createdBy, String description, int daysValid) {
        Invitation invitation = new Invitation();
        invitation.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        invitation.setTenantId(tenantId);
        invitation.setCreatedBy(createdBy);
        invitation.setDescription(description);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        invitation.setCreatedAt(LocalDateTime.now());

        return invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public Invitation validateInvitation(String code) {
        return invitationRepository.findByCode(code)
            .filter(inv -> inv.getExpiresAt().isAfter(LocalDateTime.now()))
            .orElseThrow(() -> new RuntimeException("Invalid or expired invitation"));
    }

    public Invitation useInvitation(String code, Long userId) {
        Invitation invitation = validateInvitation(code);
        invitation.setUsedBy(userId);
        invitation.setUsedAt(LocalDateTime.now());
        return invitationRepository.save(invitation);
    }

    public void deleteInvitation(Long id, Long tenantId) {
        Invitation invitation = invitationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Not authorized to delete this invitation");
        }

        invitationRepository.delete(invitation);
    }
}
