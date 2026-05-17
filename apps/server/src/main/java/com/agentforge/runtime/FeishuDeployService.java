package com.agentforge.runtime;

import com.agentforge.agent.Agent;
import com.agentforge.agent.AgentMapper;
import com.agentforge.agent.AgentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeishuDeployService {

    private static final String CHANNEL = "feishu";
    private static final Duration BIND_CODE_TTL = Duration.ofMinutes(30);
    private static final String BIND_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AgentService agentService;
    private final AgentMapper agentMapper;
    private final AgentChannelMapper channelMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final FeishuSessionStore feishuSessionStore;

    public BindCodeDto createBindCode(Long agentId, Long tenantId) {
        Agent agent = agentService.requireAgent(agentId, tenantId);
        revokePendingBindCode(agentId);

        String code = generateCode();
        BindPayload payload = new BindPayload(agentId, tenantId);
        try {
            String json = objectMapper.writeValueAsString(payload);
            redis.opsForValue().set(bindCodeKey(code), json, BIND_CODE_TTL);
            redis.opsForValue().set(agentBindCodeKey(agentId), code, BIND_CODE_TTL);
        } catch (Exception e) {
            throw new RuntimeException("生成绑定码失败");
        }

        BindCodeDto dto = new BindCodeDto();
        dto.setBindCode(code);
        dto.setCommand("绑定 " + code);
        dto.setAgentId(agentId);
        dto.setAgentName(agent.getName());
        dto.setExpiresAt(LocalDateTime.now().plus(BIND_CODE_TTL));
        dto.setExpiresInMinutes((int) BIND_CODE_TTL.toMinutes());
        log.info("Created Feishu bind code {} for agent {}", code, agentId);
        return dto;
    }

    public BindResultDto completeBindByCode(String code, String chatId) {
        if (code == null || code.isBlank() || chatId == null || chatId.isBlank()) {
            throw new RuntimeException("绑定码或群信息无效");
        }
        String normalizedCode = code.trim().toUpperCase();
        String payloadJson = redis.opsForValue().get(bindCodeKey(normalizedCode));
        if (payloadJson == null) {
            throw new RuntimeException("绑定码无效或已过期，请在控制台重新生成");
        }

        BindPayload payload;
        try {
            payload = objectMapper.readValue(payloadJson, BindPayload.class);
        } catch (Exception e) {
            throw new RuntimeException("绑定码数据异常");
        }

        Agent agent = agentService.requireAgent(payload.agentId(), payload.tenantId());
        DeploymentDto deployment = deploy(payload.agentId(), payload.tenantId(), chatId);

        redis.delete(bindCodeKey(normalizedCode));
        redis.delete(agentBindCodeKey(payload.agentId()));

        BindResultDto result = new BindResultDto();
        result.setDeployment(deployment);
        result.setAgentName(agent.getName());
        result.setMessage("绑定成功！Agent「" + agent.getName() + "」已在本群生效。");
        log.info("Feishu bind code {} completed for chat {}", normalizedCode, chatId);
        return result;
    }

    public DeploymentDto deploy(Long agentId, Long tenantId, String chatId) {
        if (chatId == null || chatId.isBlank()) {
            throw new RuntimeException("chatId 不能为空");
        }
        Agent agent = agentService.requireAgent(agentId, tenantId);
        return bindChat(agent, tenantId, chatId.trim());
    }

    private DeploymentDto bindChat(Agent agent, Long tenantId, String trimmedChatId) {
        Long agentId = agent.getId();

        AgentChannel existing = channelMapper.selectOne(
            Wrappers.<AgentChannel>lambdaQuery()
                .eq(AgentChannel::getChannel, CHANNEL)
                .eq(AgentChannel::getChatId, trimmedChatId)
        );

        if (existing != null && !existing.getAgentId().equals(agentId)) {
            throw new RuntimeException("该飞书群已绑定其他 Agent，请先解绑");
        }

        if (existing == null) {
            AgentChannel row = new AgentChannel();
            row.setAgentId(agentId);
            row.setTenantId(tenantId);
            row.setChannel(CHANNEL);
            row.setChatId(trimmedChatId);
            row.setCreatedAt(LocalDateTime.now());
            channelMapper.insert(row);
        }

        redis.opsForValue().set(feishuAgentKey(trimmedChatId), String.valueOf(agentId));
        scheduleExpireIfConfigured(agent);

        if ("draft".equals(agent.getStatus())) {
            agent.setStatus("active");
            agent.setUpdatedAt(LocalDateTime.now());
            agentMapper.updateById(agent);
        }

        log.info("Agent {} deployed to Feishu chat {}", agentId, trimmedChatId);
        return toDto(agentId, trimmedChatId, tenantId);
    }

    private void revokePendingBindCode(Long agentId) {
        String old = redis.opsForValue().get(agentBindCodeKey(agentId));
        if (old != null) {
            redis.delete(bindCodeKey(old));
        }
        redis.delete(agentBindCodeKey(agentId));
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(BIND_CODE_CHARS.charAt(RANDOM.nextInt(BIND_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private static String bindCodeKey(String code) {
        return "feishu:bind:code:" + code;
    }

    private static String agentBindCodeKey(Long agentId) {
        return "feishu:bind:agent:" + agentId;
    }

    private record BindPayload(Long agentId, Long tenantId) {
    }

    public void undeploy(Long agentId, Long tenantId, String chatId) {
        agentService.requireAgent(agentId, tenantId);
        channelMapper.delete(
            Wrappers.<AgentChannel>lambdaQuery()
                .eq(AgentChannel::getAgentId, agentId)
                .eq(AgentChannel::getChannel, CHANNEL)
                .eq(AgentChannel::getChatId, chatId.trim())
        );
        redis.delete(feishuAgentKey(chatId.trim()));
        redis.delete("agent:expire-watch:" + agentId);
        feishuSessionStore.clear(chatId.trim());
    }

    @Transactional(readOnly = true)
    public List<DeploymentDto> listDeployments(Long agentId, Long tenantId) {
        agentService.requireAgent(agentId, tenantId);
        return channelMapper.selectList(
                Wrappers.<AgentChannel>lambdaQuery()
                    .eq(AgentChannel::getAgentId, agentId)
                    .eq(AgentChannel::getChannel, CHANNEL)
            )
            .stream()
            .map(c -> toDto(c.getAgentId(), c.getChatId(), c.getTenantId()))
            .collect(Collectors.toList());
    }

    public Long resolveAgentIdByChat(String chatId) {
        String fromRedis = redis.opsForValue().get(feishuAgentKey(chatId));
        if (fromRedis != null) {
            return Long.parseLong(fromRedis);
        }
        AgentChannel row = channelMapper.selectOne(
            Wrappers.<AgentChannel>lambdaQuery()
                .eq(AgentChannel::getChannel, CHANNEL)
                .eq(AgentChannel::getChatId, chatId)
        );
        if (row != null) {
            redis.opsForValue().set(feishuAgentKey(chatId), String.valueOf(row.getAgentId()));
            return row.getAgentId();
        }
        return null;
    }

    private void scheduleExpireIfConfigured(Agent agent) {
        try {
            if (agent.getConfig() == null || agent.getConfig().isBlank()) {
                return;
            }
            JsonNode lifecycle = objectMapper.readTree(agent.getConfig()).path("lifecycle");
            if (!lifecycle.path("autoArchive").asBoolean(false) && lifecycle.path("ttlDays").isMissingNode()) {
                return;
            }
            int ttlDays = lifecycle.path("ttlDays").asInt(0);
            if (ttlDays <= 0) {
                return;
            }
            redis.opsForValue().set(
                "agent:expire-watch:" + agent.getId(),
                String.valueOf(agent.getId()),
                Duration.ofDays(ttlDays)
            );
        } catch (Exception e) {
            log.warn("Failed to schedule agent expire: {}", e.getMessage());
        }
    }

    static String feishuAgentKey(String chatId) {
        return "feishu:chat:" + chatId + ":agent";
    }

    private DeploymentDto toDto(Long agentId, String chatId, Long tenantId) {
        DeploymentDto dto = new DeploymentDto();
        dto.setAgentId(agentId);
        dto.setChannel(CHANNEL);
        dto.setChatId(chatId);
        dto.setTenantId(tenantId);
        return dto;
    }

    @Data
    public static class DeploymentDto {
        private Long agentId;
        private Long tenantId;
        private String channel;
        private String chatId;
    }

    @Data
    public static class BindCodeDto {
        private String bindCode;
        private String command;
        private Long agentId;
        private String agentName;
        private LocalDateTime expiresAt;
        private int expiresInMinutes;
    }

    @Data
    public static class BindResultDto {
        private DeploymentDto deployment;
        private String agentName;
        private String message;
    }
}
