package com.agentforge.runtime;

import com.agentforge.agent.AgentRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeishuSessionStore {

    private static final int MAX_TURNS = 10;
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public List<AgentRunService.ChatMessage> loadHistory(String chatId) {
        String key = historyKey(chatId);
        List<String> raw = redis.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<AgentRunService.ChatMessage> out = new ArrayList<>();
        for (String item : raw) {
            try {
                out.add(objectMapper.readValue(item, AgentRunService.ChatMessage.class));
            } catch (Exception ignored) {
                // skip corrupt entry
            }
        }
        return out;
    }

    public void appendTurn(String chatId, String userText, String assistantText) {
        try {
            String key = historyKey(chatId);
            AgentRunService.ChatMessage user = new AgentRunService.ChatMessage();
            user.setRole("user");
            user.setContent(userText);
            AgentRunService.ChatMessage assistant = new AgentRunService.ChatMessage();
            assistant.setRole("assistant");
            assistant.setContent(assistantText);

            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(user));
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(assistant));
            redis.opsForList().trim(key, -MAX_TURNS * 2L, -1);
            redis.expire(key, TTL);
        } catch (Exception e) {
            // non-fatal
        }
    }

    public void clear(String chatId) {
        redis.delete(historyKey(chatId));
    }

    private String historyKey(String chatId) {
        return "feishu:history:" + chatId;
    }
}
