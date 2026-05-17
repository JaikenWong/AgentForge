package com.agentforge.generator;

import com.agentforge.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratorService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GENERATOR_SYSTEM_PROMPT = """
        你是智能体生成引擎。根据用户需求生成 Agent 配置。

        必须只输出一个 JSON 对象，不要 markdown，不要代码块，不要任何前后说明文字。

        格式：
        {
          "name": "智能体名称",
          "description": "一两句话描述",
          "prompt": "完整系统提示词，含角色、目标、流程、输出格式",
          "config": {
            "temperature": 0.7,
            "max_tokens": 4096,
            "tools": []
          }
        }

        要求：name、description、prompt 均不能为空字符串。
        """;

    public GeneratorResult generateAgent(Long userId, Long tenantId, String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return GeneratorResult.error("prompt 不能为空");
        }

        try {
            String response = llmClient.chat(
                GENERATOR_SYSTEM_PROMPT,
                List.of(Map.of("role", "user", "content", "用户需求: " + userPrompt)),
                2000
            );

            String json = llmClient.extractJSON(response);
            log.debug("Generator raw json: {}", json);

            if (json == null || json.isBlank()) {
                return GeneratorResult.error("模型未返回有效 JSON，请重试");
            }

            GeneratorResult result = parseGeneratorResult(json);
            validateResult(result);
            result.setSuccess(true);
            return result;
        } catch (Exception e) {
            log.error("Failed to parse generator result: {}", e.getMessage());
            return GeneratorResult.error("生成配置失败: " + e.getMessage());
        }
    }

    private GeneratorResult parseGeneratorResult(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        GeneratorResult result = new GeneratorResult();
        result.setName(textField(root, "name"));
        result.setDescription(textField(root, "description"));
        result.setPrompt(textField(root, "prompt"));
        result.setConfig(configField(root));
        return result;
    }

    private String textField(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private String configField(JsonNode root) {
        JsonNode config = root.get("config");
        if (config == null || config.isNull()) {
            return "{\"tools\":[]}";
        }
        if (config.isTextual()) {
            return config.asText();
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return "{\"tools\":[]}";
        }
    }

    private void validateResult(GeneratorResult result) {
        if (result.getName() == null || result.getName().isBlank()) {
            throw new IllegalArgumentException("缺少 name 字段");
        }
        if (result.getDescription() == null || result.getDescription().isBlank()) {
            throw new IllegalArgumentException("缺少 description 字段");
        }
        if (result.getPrompt() == null || result.getPrompt().isBlank()) {
            throw new IllegalArgumentException("缺少 prompt 字段");
        }
        if (result.getConfig() == null || result.getConfig().isBlank()) {
            result.setConfig("{\"tools\":[]}");
        }
    }

    @lombok.Data
    public static class GeneratorResult {
        private String name;
        private String description;
        private String prompt;
        private String config;
        private Long agentId;
        private boolean success = false;
        private String error;

        public static GeneratorResult error(String error) {
            GeneratorResult result = new GeneratorResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }
    }
}
