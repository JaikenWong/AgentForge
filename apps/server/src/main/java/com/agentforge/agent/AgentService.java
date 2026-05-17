package com.agentforge.agent;

import com.agentforge.user.User;
import com.agentforge.user.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentMapper agentMapper;
    private final UserMapper userMapper;
    private final AgentTemplateService templateService;

    @Transactional(readOnly = true)
    public List<AgentDto> getAllAgents(Long tenantId) {
        return agentMapper.selectList(
                Wrappers.<Agent>lambdaQuery().eq(Agent::getTenantId, tenantId)
            )
            .stream()
            .map(AgentDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentDto getAgentById(Long id, Long tenantId) {
        return AgentDto.fromEntity(requireAgent(id, tenantId));
    }

    @Transactional(readOnly = true)
    public AgentDto getAgentByUuid(UUID uuid) {
        Agent agent = agentMapper.selectOne(
            Wrappers.<Agent>lambdaQuery().eq(Agent::getUuid, uuid.toString())
        );
        if (agent == null) {
            throw new RuntimeException("Agent not found");
        }
        return AgentDto.fromEntity(agent);
    }

    public AgentDto createAgent(Long userId, Long tenantId, String name, String description, String prompt, String config) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Agent agent = new Agent();
        agent.setUuid(UUID.randomUUID().toString());
        agent.setUserId(userId);
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setDescription(description);
        agent.setPrompt(prompt);
        agent.setConfig(config);
        agent.setStatus("draft");
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());

        agentMapper.insert(agent);
        return AgentDto.fromEntity(agent, user);
    }

    public AgentDto updateAgent(Long id, String name, String description, String prompt, String config, String status) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new RuntimeException("Agent not found");
        }
        // tenant check done in controller before update

        if (name != null) agent.setName(name);
        if (description != null) agent.setDescription(description);
        if (prompt != null) agent.setPrompt(prompt);
        if (config != null) agent.setConfig(config);
        if (status != null) agent.setStatus(status);

        agent.setUpdatedAt(LocalDateTime.now());
        agentMapper.updateById(agent);

        User user = userMapper.selectById(agent.getUserId());
        return AgentDto.fromEntity(agent, user);
    }

    public void deleteAgent(Long id, Long tenantId) {
        requireAgent(id, tenantId);
        agentMapper.deleteById(id);
    }

    public AgentDto cloneAgent(Long id, Long userId, Long tenantId) {
        Agent source = requireAgent(id, tenantId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Agent clone = new Agent();
        clone.setUuid(UUID.randomUUID().toString());
        clone.setUserId(userId);
        clone.setTenantId(tenantId);
        clone.setName(source.getName() + " (副本)");
        clone.setDescription(source.getDescription());
        clone.setPrompt(source.getPrompt());
        clone.setConfig(source.getConfig());
        clone.setStatus("draft");
        clone.setCreatedAt(LocalDateTime.now());
        clone.setUpdatedAt(LocalDateTime.now());
        agentMapper.insert(clone);
        return AgentDto.fromEntity(clone, user);
    }

    public AgentDto createFromTemplate(String templateId, Long userId, Long tenantId) {
        AgentTemplateService.AgentTemplate template = templateService.getTemplate(templateId);
        return createAgent(userId, tenantId, template.getName(), template.getDescription(),
            template.getPrompt(), template.getConfig());
    }

    public Agent requireAgent(Long id, Long tenantId) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null || !agent.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Agent not found");
        }
        return agent;
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
            dto.setUuid(UUID.fromString(agent.getUuid()));
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
