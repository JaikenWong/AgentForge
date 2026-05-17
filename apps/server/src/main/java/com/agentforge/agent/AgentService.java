package com.agentforge.agent;

import com.agentforge.user.User;
import com.agentforge.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AgentDto> getAllAgents(Long tenantId) {
        return agentRepository.findAll()
            .stream()
            .filter(a -> a.getTenantId().equals(tenantId))
            .map(AgentDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentDto getAgentById(Long id) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Agent not found"));
        return AgentDto.fromEntity(agent);
    }

    @Transactional(readOnly = true)
    public AgentDto getAgentByUuid(UUID uuid) {
        Agent agent = agentRepository.findByUuid(uuid);
        if (agent == null) {
            throw new RuntimeException("Agent not found");
        }
        return AgentDto.fromEntity(agent);
    }

    public AgentDto createAgent(Long userId, Long tenantId, String name, String description, String prompt, String config) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        Agent agent = new Agent();
        agent.setUuid(UUID.randomUUID());
        agent.setUserId(userId);
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setDescription(description);
        agent.setPrompt(prompt);
        agent.setConfig(config);
        agent.setStatus("draft");
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());

        Agent saved = agentRepository.save(agent);
        return AgentDto.fromEntity(saved, userOpt.get());
    }

    public AgentDto updateAgent(Long id, String name, String description, String prompt, String config, String status) {
        Agent agent = agentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (name != null) agent.setName(name);
        if (description != null) agent.setDescription(description);
        if (prompt != null) agent.setPrompt(prompt);
        if (config != null) agent.setConfig(config);
        if (status != null) agent.setStatus(status);

        agent.setUpdatedAt(LocalDateTime.now());
        Agent saved = agentRepository.save(agent);

        Optional<User> userOpt = userRepository.findById(agent.getUserId());
        return AgentDto.fromEntity(saved, userOpt.orElse(null));
    }

    public void deleteAgent(Long id) {
        if (!agentRepository.existsById(id)) {
            throw new RuntimeException("Agent not found");
        }
        agentRepository.deleteById(id);
    }

    @Data
    public static class AgentDto {
        private Long id;
        private UUID uuid;
        private Long userId;
        private String username;
        private Long tenantId;
        private String name;
        private String description;
        private String prompt;
        private String config;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static AgentDto fromEntity(Agent agent) {
            AgentDto dto = new AgentDto();
            dto.setId(agent.getId());
            dto.setUuid(agent.getUuid());
            dto.setUserId(agent.getUserId());
            dto.setTenantId(agent.getTenantId());
            dto.setName(agent.getName());
            dto.setDescription(agent.getDescription());
            dto.setPrompt(agent.getPrompt());
            dto.setConfig(agent.getConfig());
            dto.setStatus(agent.getStatus());
            dto.setCreatedAt(agent.getCreatedAt());
            dto.setUpdatedAt(agent.getUpdatedAt());
            return dto;
        }

        public static AgentDto fromEntity(Agent agent, User user) {
            AgentDto dto = fromEntity(agent);
            if (user != null) {
                dto.setUsername(user.getUsername());
            }
            return dto;
        }
    }
}
