package com.agentforge.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final InvitationMapper invitationMapper;

    @Transactional(readOnly = true)
    public List<Invitation> getInvitations(Long tenantId) {
        return invitationMapper.selectList(
            Wrappers.<Invitation>lambdaQuery().eq(Invitation::getTenantId, tenantId)
        );
    }

    public Invitation createInvitation(Long tenantId, Long createdBy, String description, int daysValid) {
        Invitation invitation = new Invitation();
        invitation.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        invitation.setTenantId(tenantId);
        invitation.setCreatedBy(createdBy);
        invitation.setDescription(description);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(daysValid));
        invitation.setCreatedAt(LocalDateTime.now());

        invitationMapper.insert(invitation);
        return invitation;
    }

    @Transactional(readOnly = true)
    public Invitation validateInvitation(String code) {
        Invitation invitation = invitationMapper.selectOne(
            Wrappers.<Invitation>lambdaQuery().eq(Invitation::getCode, code)
        );
        if (invitation == null || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invalid or expired invitation");
        }
        return invitation;
    }

    public Invitation useInvitation(String code, Long userId) {
        Invitation invitation = validateInvitation(code);
        invitation.setUsedBy(userId);
        invitation.setUsedAt(LocalDateTime.now());
        invitationMapper.updateById(invitation);
        return invitation;
    }

    public void deleteInvitation(Long id, Long tenantId) {
        Invitation invitation = invitationMapper.selectById(id);
        if (invitation == null) {
            throw new RuntimeException("Invitation not found");
        }

        if (!invitation.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Not authorized to delete this invitation");
        }

        invitationMapper.deleteById(id);
    }
}
