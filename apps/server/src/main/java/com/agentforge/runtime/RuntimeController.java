package com.agentforge.runtime;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime")
@RequiredArgsConstructor
public class RuntimeController {

    private final StringRedisTemplate redis;

    @Value("${feishu.app-id:}")
    private String feishuAppId;

    @Value("${feishu.app-secret:}")
    private String feishuAppSecret;

    @Value("${feishu.encrypt-key:}")
    private String feishuEncryptKey;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @GetMapping("/status")
    public ResponseEntity<RuntimeStatus> status() {
        RuntimeStatus s = new RuntimeStatus();
        s.setFeishuAppConfigured(feishuAppId != null && !feishuAppId.isBlank()
            && feishuAppSecret != null && !feishuAppSecret.isBlank());
        s.setFeishuEncryptKeyConfigured(feishuEncryptKey != null && !feishuEncryptKey.isBlank());
        s.setQueueDepth(queueDepth());
        s.setWebhookPath("/webhook/feishu");
        s.setPublicBaseUrl(publicBaseUrl != null && !publicBaseUrl.isBlank() ? publicBaseUrl.replaceAll("/+$", "") : null);
        if (s.getPublicBaseUrl() != null) {
            s.setWebhookUrl(s.getPublicBaseUrl() + s.getWebhookPath());
        }
        return ResponseEntity.ok(s);
    }

    private long queueDepth() {
        Long size = redis.opsForList().size("agent:queue");
        return size != null ? size : 0;
    }

    @Data
    public static class RuntimeStatus {
        private boolean feishuAppConfigured;
        private boolean feishuEncryptKeyConfigured;
        private long queueDepth;
        private String webhookPath;
        private String publicBaseUrl;
        private String webhookUrl;
    }
}
