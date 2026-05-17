package com.agentforge.runtime;

import com.agentforge.agent.Agent;
import com.agentforge.agent.AgentMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentArchiveScheduler {

    private final AgentMapper agentMapper;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void archiveExpiredAgents() {
        List<Agent> candidates = agentMapper.selectList(
            Wrappers.<Agent>lambdaQuery().in(Agent::getStatus, "active", "draft")
        );

        LocalDateTime now = LocalDateTime.now();
        int archived = 0;

        for (Agent agent : candidates) {
            Integer ttlDays = readTtlDays(agent);
            if (ttlDays == null || ttlDays <= 0) {
                continue;
            }
            LocalDateTime expireAt = agent.getCreatedAt().plusDays(ttlDays);
            if (expireAt.isBefore(now)) {
                agent.setStatus("archived");
                agent.setUpdatedAt(now);
                agentMapper.updateById(agent);
                archived++;
                log.info("Auto-archived agent {} (ttl {} days)", agent.getId(), ttlDays);
            }
        }

        if (archived > 0) {
            log.info("Auto-archive completed: {} agents", archived);
        }
    }

    private Integer readTtlDays(Agent agent) {
        try {
            if (agent.getConfig() == null || agent.getConfig().isBlank()) {
                return null;
            }
            JsonNode lifecycle = objectMapper.readTree(agent.getConfig()).path("lifecycle");
            if (!lifecycle.path("autoArchive").asBoolean(false) && lifecycle.path("ttlDays").isMissingNode()) {
                return null;
            }
            int days = lifecycle.path("ttlDays").asInt(0);
            return days > 0 ? days : null;
        } catch (Exception e) {
            return null;
        }
    }
}
