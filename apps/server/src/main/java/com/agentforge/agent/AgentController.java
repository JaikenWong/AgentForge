package com.agentforge.agent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AgentRunService agentRunService;
    private final AgentTemplateService templateService;

    @GetMapping("/templates")
    public ResponseEntity<List<AgentTemplateService.AgentTemplate>> listTemplates() {
        return ResponseEntity.ok(templateService.listTemplates());
    }

    @PostMapping("/from-template")
    public ResponseEntity<AgentService.AgentDto> createFromTemplate(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody FromTemplateRequest request) {
        return ResponseEntity.ok(agentService.createFromTemplate(request.getTemplateId(), userId, tenantId));
    }

    @GetMapping
    public ResponseEntity<List<AgentService.AgentDto>> getAllAgents(@RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(agentService.getAllAgents(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentService.AgentDto> getAgentById(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(agentService.getAgentById(id, tenantId));
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<AgentService.AgentDto> getAgentByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(agentService.getAgentByUuid(uuid));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<AgentRunService.ExecutionLogDto>> getLogs(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(agentRunService.getLogs(id, tenantId, limit));
    }

    @PostMapping("/{id}/chat")
    public ResponseEntity<AgentRunService.ChatResult> chat(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody AgentRunService.ChatRequest request) {
        return ResponseEntity.ok(agentRunService.chat(id, userId, tenantId, "web", request));
    }

    @PostMapping("/{id}/invoke")
    public ResponseEntity<AgentRunService.ChatResult> invoke(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody AgentRunService.ChatRequest request) {
        return ResponseEntity.ok(agentRunService.chat(id, userId, tenantId, "api", request));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<AgentService.AgentDto> cloneAgent(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(agentService.cloneAgent(id, userId, tenantId));
    }

    @PostMapping
    public ResponseEntity<AgentService.AgentDto> createAgent(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody AgentRequest request) {
        AgentService.AgentDto agent = agentService.createAgent(
            userId, tenantId, request.getName(), request.getDescription(), request.getPrompt(), request.getConfig()
        );
        return ResponseEntity.ok(agent);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentService.AgentDto> updateAgent(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody AgentRequest request) {
        agentService.requireAgent(id, tenantId);
        AgentService.AgentDto agent = agentService.updateAgent(
            id, request.getName(), request.getDescription(), request.getPrompt(), request.getConfig(), request.getStatus()
        );
        return ResponseEntity.ok(agent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId) {
        agentService.deleteAgent(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class AgentRequest {
        private String name;
        private String description;
        private String prompt;
        private String config;
        private String status;
    }

    @Data
    public static class FromTemplateRequest {
        private String templateId;
    }
}
