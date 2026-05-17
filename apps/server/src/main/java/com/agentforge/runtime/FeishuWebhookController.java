package com.agentforge.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class FeishuWebhookController {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final FeishuDeployService feishuDeployService;
    private final FeishuBotService feishuBotService;

    @Value("${feishu.encrypt-key:}")
    private String encryptKey;

    @PostMapping("/feishu")
    public ResponseEntity<Map<String, Object>> handleFeishu(
            @RequestHeader(value = "X-Lark-Signature", required = false) String signature,
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody) throws Exception {

        JsonNode body = objectMapper.readTree(rawBody);

        if ("url_verification".equals(body.path("type").asText())) {
            return ResponseEntity.ok(Map.of("challenge", body.path("challenge").asText()));
        }

        if (encryptKey != null && !encryptKey.isBlank()) {
            if (signature == null || timestamp == null || !verifySignature(signature, timestamp, rawBody)) {
                log.warn("Feishu webhook signature invalid");
                return ResponseEntity.status(401).body(Map.of("code", -1));
            }
        }

        if ("im.message.receive_v1".equals(body.path("header").path("event_type").asText())) {
            handleMessageEvent(body);
        }

        return ResponseEntity.ok(Map.of("code", 0));
    }

    private void handleMessageEvent(JsonNode body) {
        try {
            JsonNode message = body.path("event").path("message");
            String messageId = message.path("message_id").asText();
            String chatId = message.path("chat_id").asText();
            String messageType = message.path("message_type").asText();

            if (messageId.isBlank() || chatId.isBlank()) {
                return;
            }

            Boolean processed = redis.opsForValue().setIfAbsent(
                "feishu:processed:" + messageId, "1", Duration.ofMinutes(5)
            );
            if (Boolean.FALSE.equals(processed)) {
                return;
            }

            String content = extractText(messageType, message.path("content").asText());
            if (content.isBlank()) {
                return;
            }

            Optional<String> bindCode = FeishuBindCodeParser.parse(content);
            if (bindCode.isPresent()) {
                handleBindCommand(bindCode.get(), chatId, messageId);
                return;
            }

            Long agentId = feishuDeployService.resolveAgentIdByChat(chatId);
            if (agentId == null) {
                log.debug("No agent bound for Feishu chat {}", chatId);
                return;
            }

            String userId = body.path("event").path("sender").path("sender_id").path("user_id").asText("");

            Map<String, Object> job = Map.of(
                "id", messageId,
                "agent_id", agentId,
                "channel", "feishu",
                "channel_ctx", Map.of(
                    "group_id", chatId,
                    "message_id", messageId,
                    "user_id", userId
                ),
                "content", content,
                "role", "user"
            );

            redis.opsForList().leftPush("agent:queue", objectMapper.writeValueAsString(job));
            log.debug("Queued Feishu message for agent {}", agentId);
        } catch (Exception e) {
            log.error("Feishu message event failed: {}", e.getMessage());
        }
    }

    private void handleBindCommand(String code, String chatId, String messageId) {
        String reply;
        try {
            FeishuDeployService.BindResultDto result = feishuDeployService.completeBindByCode(code, chatId);
            reply = result.getMessage() + "\n现在可以直接向我提问。";
        } catch (Exception e) {
            reply = "绑定失败：" + e.getMessage();
            log.warn("Bind failed for code {} chat {}: {}", code, chatId, e.getMessage());
        }

        try {
            if (!messageId.isBlank()) {
                feishuBotService.replyToMessage(messageId, reply);
            } else {
                feishuBotService.sendTextToChat(chatId, reply);
            }
        } catch (Exception e) {
            log.error("Failed to send bind reply: {}", e.getMessage());
        }
    }

    private String extractText(String messageType, String contentRaw) {
        if (!"text".equals(messageType) || contentRaw == null || contentRaw.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readTree(contentRaw).path("text").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private boolean verifySignature(String signature, String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + body).getBytes(StandardCharsets.UTF_8));
        String expected = HexFormat.of().formatHex(digest);
        return expected.equals(signature);
    }
}
