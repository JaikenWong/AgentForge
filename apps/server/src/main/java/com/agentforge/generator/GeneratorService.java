package com.agentforge.generator;

import com.agentforge.agent.Agent;
import com.agentforge.agent.AgentRepository;
import com.agentforge.intent.IntentService;
import com.agentforge.llm.LlmClient;
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
    private final AgentRepository agentRepository;

    private static final String GENERATOR_SYSTEM_PROMPT = """
        你是一个智能体生成引擎。根据用户需求生成完整的智能体配置。

        请以JSON格式输出：
        {
          "name": "智能体名称",
          "description": "智能体描述",
          "prompt": "智能体核心提示词",
          "config": {
            "temperature": 0.7,
            "max_tokens": 4096,
            "tools": []
          }
        }

        要求：
        1. name: 简洁明了的名称
        2. description: 1-2句话描述智能体功能
        3. prompt: 详细的系统提示词，包含角色定义、任务目标、工作流程、输出格式
        4. config: JSON配置对象
        """;

    public GeneratorResult generateAgent(Long userId, Long tenantId, String userPrompt) {
        String response = llmClient.chat(GENERATOR_SYSTEM_PROMPT,
            List.of(Map.of("role", "user", "content", "生成智能体: " + userPrompt)),
            2000);

        try {
            String json = llmClient.extractJSON(response);
            log.debug("Generator result: {}", json);

            GeneratorResult result = parseGeneratorResult(json);
            if (result != null) {
                // Save agent to database
                Agent agent = new Agent();
                agent.setUuid(java.util.UUID.randomUUID());
                agent.setTenantId(tenantId);
                agent.setName(result.getName());
                agent.setDescription(result.getDescription());
                agent.setPrompt(result.getPrompt());
                agent.setConfig(result.getConfig());
                agent.setStatus("draft");

                agentRepository.save(agent);
                result.setAgentId(agent.getId());
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to parse generator result: {}", e.getMessage());
            return GeneratorResult.error(e.getMessage());
        }
    }

    private GeneratorResult parseGeneratorResult(String json) {
        GeneratorResult result = new GeneratorResult();

        // Simple JSON parsing
        if (json.contains("\"name\":")) {
            result.setName(extractString(json, "name"));
        }
        if (json.contains("\"description\":")) {
            result.setDescription(extractString(json, "description"));
        }
        if (json.contains("\"prompt\":")) {
            result.setPrompt(extractString(json, "prompt"));
        }
        if (json.contains("\"config\":")) {
            result.setConfig(extractObject(json, "config"));
        }

        return result;
    }

    private String extractString(String json, String key) {
        int start = json.indexOf("\"" + key + "\":") + key.length() + 4;
        int end = json.indexOf("\"", start);
        if (end > start) {
            return json.substring(start, end);
        }
        return "";
    }

    private String extractObject(String json, String key) {
        int start = json.indexOf("\"" + key + "\":");
        if (start < 0) return "{}";
        start = json.indexOf("{", start);
        if (start < 0) return "{}";

        int braceCount = 1;
        int end = start + 1;
        while (end < json.length() && braceCount > 0) {
            char c = json.charAt(end);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            end++;
        }

        if (braceCount == 0) {
            return json.substring(start, end);
        }
        return "{}";
    }

    @lombok.Data
    public static class GeneratorResult {
        private String name;
        private String description;
        private String prompt;
        private String config;
        private Long agentId;
        private boolean success;
        private String error;

        public static GeneratorResult error(String error) {
            GeneratorResult result = new GeneratorResult();
            result.setSuccess(false);
            result.setError(error);
            return result;
        }
    }
}
