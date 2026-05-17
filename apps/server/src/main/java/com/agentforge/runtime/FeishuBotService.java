package com.agentforge.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuBotService {

    private static final String TENANT_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal";
    private static final String SEND_MSG_URL = "https://open.feishu.cn/open-apis/im/v1/messages";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Value("${feishu.app-id:}")
    private String appId;

    @Value("${feishu.app-secret:}")
    private String appSecret;

    public void replyToMessage(String messageId, String text) {
        if (messageId == null || messageId.isBlank() || text == null || text.isBlank()) {
            return;
        }
        requireConfigured();
        try {
            String token = getTenantAccessToken();
            String contentJson = objectMapper.writeValueAsString(Map.of("text", text));
            String path = SEND_MSG_URL + "/" + messageId + "/reply";

            String response = RestClient.create().post()
                .uri(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("msg_type", "text", "content", contentJson))
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            if (root.path("code").asInt(-1) != 0) {
                throw new RuntimeException("Feishu reply failed: " + root.path("msg").asText());
            }
            log.debug("Feishu reply sent for message {}", messageId);
        } catch (Exception e) {
            log.error("Failed to reply Feishu message: {}", e.getMessage());
            throw new RuntimeException("飞书回复失败: " + e.getMessage(), e);
        }
    }

    public void sendTextToChat(String chatId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        requireConfigured();
        try {
            String token = getTenantAccessToken();
            String contentJson = objectMapper.writeValueAsString(Map.of("text", text));

            String response = RestClient.create().post()
                .uri(uri -> uri.path(SEND_MSG_URL).queryParam("receive_id_type", "chat_id").build())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "receive_id", chatId,
                    "msg_type", "text",
                    "content", contentJson
                ))
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            if (root.path("code").asInt(-1) != 0) {
                throw new RuntimeException("Feishu send failed: " + root.path("msg").asText());
            }
            log.debug("Feishu message sent to chat {}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Feishu message: {}", e.getMessage());
            throw new RuntimeException("飞书发消息失败: " + e.getMessage(), e);
        }
    }

    private String getTenantAccessToken() throws Exception {
        String cached = redis.opsForValue().get("feishu:tenant_access_token");
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        String response = RestClient.create().post()
            .uri(TENANT_TOKEN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("app_id", appId, "app_secret", appSecret))
            .retrieve()
            .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        if (root.path("code").asInt(-1) != 0) {
            throw new RuntimeException("Feishu tenant token error: " + root.path("msg").asText());
        }

        String token = root.path("tenant_access_token").asText();
        int expire = root.path("expire").asInt(7200);
        redis.opsForValue().set("feishu:tenant_access_token", token, Duration.ofSeconds(Math.max(expire - 120, 60)));
        return token;
    }

    private void requireConfigured() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("FEISHU_APP_ID / FEISHU_APP_SECRET 未配置");
        }
    }
}
