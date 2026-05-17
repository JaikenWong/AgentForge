package com.agentforge.runtime;

import com.agentforge.agent.Agent;
import com.agentforge.agent.AgentMapper;
import com.agentforge.agent.AgentRunService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentQueueProcessor {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AgentRunService agentRunService;
    private final AgentMapper agentMapper;
    private final FeishuBotService feishuBotService;
    private final FeishuSessionStore feishuSessionStore;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread workerThread;

    @PostConstruct
    void start() {
        workerThread = new Thread(this::loop, "agent-queue-worker");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("Agent queue processor started");
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    private void loop() {
        while (running.get()) {
            try {
                String payload = redis.opsForList().rightPop("agent:queue", Duration.ofSeconds(3));
                if (payload == null) {
                    continue;
                }
                processJob(payload);
            } catch (Exception e) {
                if (running.get()) {
                    log.error("Queue processor error: {}", e.getMessage());
                }
            }
        }
    }

    private void processJob(String payload) {
        try {
            JsonNode job = objectMapper.readTree(payload);
            long agentId = job.path("agent_id").asLong();
            String channel = job.path("channel").asText("web");
            String content = job.path("content").asText();
            JsonNode ctx = job.path("channel_ctx");

            Agent agent = agentMapper.selectById(agentId);
            if (agent == null) {
                log.warn("Agent {} not found, skip job", agentId);
                return;
            }

            AgentRunService.ChatRequest request = new AgentRunService.ChatRequest();
            request.setMessage(content);

            String chatId = ctx.path("group_id").asText("");
            String messageId = ctx.path("message_id").asText("");

            if ("feishu".equals(channel) && !chatId.isBlank()) {
                request.setHistory(feishuSessionStore.loadHistory(chatId));
            } else {
                request.setHistory(null);
            }

            AgentRunService.ChatResult result = agentRunService.chat(
                agentId, null, agent.getTenantId(), channel, request
            );

            if ("feishu".equals(channel) && !chatId.isBlank()) {
                feishuSessionStore.appendTurn(chatId, content, result.getReply());
                try {
                    if (!messageId.isBlank()) {
                        feishuBotService.replyToMessage(messageId, result.getReply());
                    } else {
                        feishuBotService.sendTextToChat(chatId, result.getReply());
                    }
                } catch (Exception replyErr) {
                    log.warn("Feishu reply failed, fallback to chat send: {}", replyErr.getMessage());
                    feishuBotService.sendTextToChat(chatId, result.getReply());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process queue job: {}", e.getMessage());
        }
    }
}
