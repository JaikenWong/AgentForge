package com.agentforge.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public LlmClient(
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.model}") String model) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
    }

    public String chat(String systemPrompt, List<Map<String, String>> messages, int maxTokens) {
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

        try {
            String response = restClient.post()
                .uri("/v1/messages")
                .headers(h -> h.addAll(headers))
                .body(body)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            if (content.isArray() && content.size() > 0) {
                return content.get(0).path("text").asText();
            }
            return "";
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage());
        }
    }

    public String extractJSON(String text) {
        // Try direct parse
        try {
            objectMapper.readTree(text);
            return text;
        } catch (Exception ignored) {}

        // Extract from ```json blocks
        var jsonBlockMatch = text.matches(".*```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```.*");
        if (jsonBlockMatch) {
            String extracted = text.replaceAll(".*```(?:json)?\\s*\\n?", "")
                .replaceAll("\\n?```.*", "").trim();
            try {
                objectMapper.readTree(extracted);
                return extracted;
            } catch (Exception ignored) {}
        }

        // Find first { ... }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return text;
    }
}