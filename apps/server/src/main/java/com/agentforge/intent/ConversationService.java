package com.agentforge.intent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(Long userId, Long tenantId) {
        return conversationRepository.findByUserIdAndTenantId(userId, tenantId)
            .stream()
            .map(ConversationDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversation(Long id) {
        Conversation conv = conversationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return ConversationDto.fromEntity(conv);
    }

    public ConversationDto createConversation(Long userId, Long tenantId, String type) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTenantId(tenantId);
        conv.setType(type);
        conv.setStatus("active");
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());

        Conversation saved = conversationRepository.save(conv);
        return ConversationDto.fromEntity(saved);
    }

    public ConversationDto addMessage(Long conversationId, String role, String content, String metadata) {
        Conversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));

        ConversationMessage msg = new ConversationMessage();
        msg.setConversation(conv);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMetadata(metadata);
        msg.setCreatedAt(LocalDateTime.now());

        messageRepository.save(msg);
        conv.getMessages().add(msg);
        conv.setUpdatedAt(LocalDateTime.now());

        return ConversationDto.fromEntity(conv);
    }

    public ConversationDto completeConversation(Long id) {
        Conversation conv = conversationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conv.setStatus("completed");
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);
        return ConversationDto.fromEntity(conv);
    }

    public ConversationDto attachAgent(Long id, String agentUuid) {
        Conversation conv = conversationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        conv.setAgentId(agentUuid);
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);
        return ConversationDto.fromEntity(conv);
    }

    @Data
    public static class ConversationDto {
        private Long id;
        private Long userId;
        private Long tenantId;
        private String type;
        private String status;
        private String agentId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<MessageDto> messages;

        public static ConversationDto fromEntity(Conversation conv) {
            ConversationDto dto = new ConversationDto();
            dto.setId(conv.getId());
            dto.setUserId(conv.getUserId());
            dto.setTenantId(conv.getTenantId());
            dto.setType(conv.getType());
            dto.setStatus(conv.getStatus());
            dto.setAgentId(conv.getAgentId());
            dto.setCreatedAt(conv.getCreatedAt());
            dto.setUpdatedAt(conv.getUpdatedAt());

            if (conv.getMessages() != null) {
                dto.setMessages(conv.getMessages().stream()
                    .map(MessageDto::fromEntity)
                    .collect(Collectors.toList()));
            }

            return dto;
        }
    }

    @Data
    public static class MessageDto {
        private Long id;
        private Long conversationId;
        private String role;
        private String content;
        private String metadata;
        private LocalDateTime createdAt;

        public static MessageDto fromEntity(ConversationMessage msg) {
            MessageDto dto = new MessageDto();
            dto.setId(msg.getId());
            dto.setConversationId(msg.getConversation().getId());
            dto.setRole(msg.getRole());
            dto.setContent(msg.getContent());
            dto.setMetadata(msg.getMetadata());
            dto.setCreatedAt(msg.getCreatedAt());
            return dto;
        }
    }
}
