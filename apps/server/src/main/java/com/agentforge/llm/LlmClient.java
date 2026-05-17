package com.agentforge.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean openAiProtocol;

    public LlmClient(
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model) {
        this.restClient = RestClient.builder().baseUrl(normalizeBaseUrl(baseUrl)).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
        this.openAiProtocol = isOpenAiBaseUrl(baseUrl);
        log.info("LLM client ready: protocol={}, baseUrl={}", openAiProtocol ? "openai" : "anthropic", baseUrl);
    }

    /** 讯飞 MaaS: /v2 = OpenAI 协议, /anthropic = Anthropic 协议 */
    private static boolean isOpenAiBaseUrl(String baseUrl) {
        if (baseUrl == null) return false;
        String trimmed = baseUrl.replaceAll("/+$", "");
        return trimmed.endsWith("/v2");
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }

    public String chat(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        try {
            String response = openAiProtocol
                ? callOpenAi(systemPrompt, messages, maxTokens)
                : callAnthropic(systemPrompt, messages, maxTokens);
            return extractText(response);
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage());
        }
    }

    private String callAnthropic(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "system", systemPrompt,
            "messages", messages
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        return restClient.post()
            .uri("/v1/messages")
            .headers(h -> h.addAll(headers))
            .body(body)
            .retrieve()
            .body(String.class);
    }

    private String callOpenAi(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
        List<Map<String, String>> openAiMessages = new ArrayList<>();
        openAiMessages.add(Map.of("role", "system", "content", systemPrompt));
        openAiMessages.addAll(messages);

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", maxTokens,
            "messages", openAiMessages
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        return restClient.post()
            .uri("/chat/completions")
            .headers(h -> h.addAll(headers))
            .body(body)
            .retrieve()
            .body(String.class);
    }

    private String extractText(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (openAiProtocol) {
            return root.path("choices").path(0).path("message").path("content").asText("");
        }
        JsonNode content = root.path("content");
        if (content.isArray() && !content.isEmpty()) {
            return content.get(0).path("text").asText("");
        }
        return "";
    }

    public String extractJSON(String text) {
        try {
            objectMapper.readTree(text);
            return text;
        } catch (Exception ignored) {}

        Matcher block = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE)
            .matcher(text);
        if (block.find()) {
            String extracted = block.group(1).trim();
            try {
                objectMapper.readTree(extracted);
                return extracted;
            } catch (Exception ignored) {}
        }

        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }
}
