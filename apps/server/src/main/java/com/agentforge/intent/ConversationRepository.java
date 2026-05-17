package com.agentforge.intent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdAndTenantId(Long userId, Long tenantId);
    List<Conversation> findByTenantId(Long tenantId);
}
