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

    @GetMapping
    public ResponseEntity<List<AgentService.AgentDto>> getAllAgents(@RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(agentService.getAllAgents(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentService.AgentDto> getAgentById(@PathVariable Long id) {
        return ResponseEntity.ok(agentService.getAgentById(id));
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<AgentService.AgentDto> getAgentByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(agentService.getAgentByUuid(uuid));
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
            @RequestBody AgentRequest request) {
        AgentService.AgentDto agent = agentService.updateAgent(
            id, request.getName(), request.getDescription(), request.getPrompt(), request.getConfig(), request.getStatus()
        );
        return ResponseEntity.ok(agent);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable Long id) {
        agentService.deleteAgent(id);
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
}
