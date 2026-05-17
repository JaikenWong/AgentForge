package com.agentforge.intent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping
    public ResponseEntity<List<ConversationService.ConversationDto>> getConversations(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(conversationService.getConversations(userId, tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationService.ConversationDto> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(conversationService.getConversation(id));
    }

    @PostMapping
    public ResponseEntity<ConversationService.ConversationDto> createConversation(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody ConversationRequest request) {
        ConversationService.ConversationDto conv = conversationService.createConversation(
            userId, tenantId, request.getType()
        );
        return ResponseEntity.ok(conv);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ConversationService.ConversationDto> addMessage(
            @PathVariable Long id,
            @RequestBody MessageRequest request) {
        ConversationService.ConversationDto conv = conversationService.addMessage(
            id, request.getRole(), request.getContent(), request.getMetadata()
        );
        return ResponseEntity.ok(conv);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ConversationService.ConversationDto> completeConversation(@PathVariable Long id) {
        ConversationService.ConversationDto conv = conversationService.completeConversation(id);
        return ResponseEntity.ok(conv);
    }

    @PostMapping("/{id}/attach-agent")
    public ResponseEntity<ConversationService.ConversationDto> attachAgent(
            @PathVariable Long id,
            @RequestBody AttachAgentRequest request) {
        ConversationService.ConversationDto conv = conversationService.attachAgent(id, request.getAgentUuid());
        return ResponseEntity.ok(conv);
    }

    @Data
    public static class AttachAgentRequest {
        private String agentUuid;
    }

    @Data
    public static class ConversationRequest {
        private String type = "create_agent";
    }

    @Data
    public static class MessageRequest {
        private String role;
        private String content;
        private String metadata;
    }
}
