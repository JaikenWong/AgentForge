package com.agentforge.intent;

import com.agentforge.llm.LlmClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentService {

    private final LlmClient llmClient;

    private static final String INTENT_SYSTEM_PROMPT = """
        你是一个意图识别引擎。你的任务是分析用户输入，识别其意图类型和关键实体。

        意图类型：
        - CREATE_AGENT: 创建新智能体
        - EDIT_AGENT: 编辑现有智能体
        - DELETE_AGENT: 删除智能体
        - CHAT: 普通对话
        - QUERY_AGENTS: 查询智能体列表
        - UNKNOWN: 无法识别的意图

        请以JSON格式输出：
        {
          "intent": "意图类型",
          "confidence": 0.95,
          "entities": {
            "agent_name": "智能体名称",
            "action": "具体操作"
          },
          "clarification_needed": false,
          "clarification_question": "如果需要澄清，请提出问题"
        }
        """;

    public IntentResult analyzeIntent(String userInput, String conversationHistory) {
        String prompt = "用户输入: " + userInput + "\n\n对话历史: " + conversationHistory;

        String response = llmClient.chat(INTENT_SYSTEM_PROMPT, List.of(Map.of("role", "user", "content", prompt)), 500);

        try {
            String json = llmClient.extractJSON(response);
            log.debug("Intent analysis result: {}", json);
            return parseIntentResult(json);
        } catch (Exception e) {
            log.error("Failed to parse intent result: {}", e.getMessage());
            return IntentResult.unknown();
        }
    }

    private IntentResult parseIntentResult(String json) {
        // Simple JSON parsing without external library
        IntentResult result = new IntentResult();

        if (json.contains("\"intent\": \"CREATE_AGENT\"")) {
            result.setIntent("CREATE_AGENT");
            result.setConfidence(0.9);
        } else if (json.contains("\"intent\": \"EDIT_AGENT\"")) {
            result.setIntent("EDIT_AGENT");
            result.setConfidence(0.85);
        } else if (json.contains("\"intent\": \"DELETE_AGENT\"")) {
            result.setIntent("DELETE_AGENT");
            result.setConfidence(0.8);
        } else if (json.contains("\"intent\": \"CHAT\"")) {
            result.setIntent("CHAT");
            result.setConfidence(0.95);
        } else if (json.contains("\"intent\": \"QUERY_AGENTS\"")) {
            result.setIntent("QUERY_AGENTS");
            result.setConfidence(0.88);
        } else {
            result.setIntent("UNKNOWN");
            result.setConfidence(0.5);
        }

        result.setClarificationNeeded(json.contains("\"clarification_needed\": true"));
        if (json.contains("\"clarification_question\":")) {
            int start = json.indexOf("\"clarification_question\":") + 23;
            int end = json.indexOf("\"", start + 1);
            if (end > start) {
                result.setClarificationQuestion(json.substring(start, end));
            }
        }

        return result;
    }

    @Data
    public static class IntentResult {
        private String intent;
        private double confidence;
        private boolean clarificationNeeded;
        private String clarificationQuestion;

        public static IntentResult unknown() {
            IntentResult result = new IntentResult();
            result.setIntent("UNKNOWN");
            result.setConfidence(0.0);
            result.setClarificationNeeded(false);
            return result;
        }
    }

    @lombok.Data
    public static class Entity {
        private String name;
        private String value;
    }
}
