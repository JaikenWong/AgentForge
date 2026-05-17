package com.agentforge.intent;

import com.agentforge.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentService {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INTENT_SYSTEM_PROMPT = """
        你是意图识别引擎。判断用户当前这句话的主要意图，严格区分「闲聊」与「要创建 Agent」。

        意图类型：
        - CREATE_AGENT: 用户明确要新建/创建/生成/做一个 Agent、机器人、助手、智能体（含飞书机器人、客服等）
        - EDIT_AGENT: 修改已有 Agent
        - DELETE_AGENT: 删除 Agent
        - QUERY_AGENTS: 查看、列出已有 Agent
        - CHAT: 打招呼、闲聊、无关问题、咨询平台能力但未表达创建诉求
        - UNKNOWN: 无法判断

        规则：
        1. 仅当用户表达「想要一个…机器人/Agent/助手」或同义创建诉求时，才标 CREATE_AGENT
        2. 「你好」「在吗」「谢谢」等一律 CHAT
        3. 模糊讨论行业/技术、未说要「做/建/生成」时，标 CHAT 并在 clarification_question 中友好引导

        输出严格 JSON（不要 markdown）：
        {
          "intent": "CREATE_AGENT|CHAT|...",
          "confidence": 0.0,
          "clarification_needed": false,
          "clarification_question": ""
        }
        """;

    private static final String CLARIFY_AGENT_PROMPT = """
        你是 Agent 需求分析师。用户已明确要创建 Agent，请分析并决定是否还需澄清。

        规则：
        1. 若目标、场景、渠道等仍不清晰，clarification_needed 为 true，给出 1 个最关键问题
        2. 若信息已足够开始生成配置，clarification_needed 为 false，questions 为空数组
        3. 已澄清 2 轮以上时，尽量不再追问，直接 ready

        输出严格 JSON：
        {
          "goal": "结构化目标一句话",
          "confidence": 0.0,
          "clarification_needed": false,
          "questions": ["问题1"],
          "extracted": {
            "domain": "",
            "trigger": "",
            "behavior": ""
          }
        }
        """;

    private static final String CHAT_SYSTEM_PROMPT = """
        你是 AgentForge 平台的对话助手。用户当前只是在闲聊或咨询，尚未进入创建 Agent 流程。
        用简短、友好的中文回复（2-4 句）。
        若合适，可提示：当有创建诉求时，可以说「帮我做一个xxx机器人」。
        不要替用户生成 Agent 配置，不要输出 JSON。
        """;

    public IntentResult analyzeIntent(String userInput, String conversationHistory) {
        String prompt = "用户输入: " + userInput + "\n\n对话历史:\n" + conversationHistory;

        try {
            String response = llmClient.chat(INTENT_SYSTEM_PROMPT, List.of(Map.of("role", "user", "content", prompt)), 400);
            String json = llmClient.extractJSON(response);
            log.debug("Intent analysis result: {}", json);
            return parseIntentResult(json);
        } catch (Exception e) {
            log.error("Failed to parse intent result: {}", e.getMessage());
            return IntentResult.unknown();
        }
    }

    public ClarifyController.ClarifyResult clarifyForCreateAgent(String message, List<Map<String, String>> history) {
        String historyText = formatHistory(history);
        String prompt = "用户目标: " + message + "\n\n对话历史:\n" + historyText
            + "\n\n历史轮数(不含欢迎语): " + history.size();

        ClarifyController.ClarifyResult out = new ClarifyController.ClarifyResult();
        out.setIntent("CREATE_AGENT");
        out.setGoal(message);
        out.setExtracted(new HashMap<>());

        try {
            String response = llmClient.chat(CLARIFY_AGENT_PROMPT, List.of(Map.of("role", "user", "content", prompt)), 800);
            String json = llmClient.extractJSON(response);
            log.debug("Agent clarify result: {}", json);
            JsonNode root = objectMapper.readTree(json);

            if (root.hasNonNull("goal")) {
                out.setGoal(root.get("goal").asText());
            }
            out.setConfidence(root.path("confidence").asDouble(0.75));

            List<String> questions = new ArrayList<>();
            if (root.path("clarification_needed").asBoolean(false)) {
                if (root.has("questions") && root.get("questions").isArray() && root.get("questions").size() > 0) {
                    root.get("questions").forEach(q -> questions.add(q.asText()));
                } else if (root.hasNonNull("clarification_question")) {
                    questions.add(root.get("clarification_question").asText());
                }
            } else if (history.size() < 2 && out.getConfidence() < 0.75) {
                questions.add("请补充：这个 Agent 主要服务谁？要部署到飞书群还是网站？");
            }

            if (history.size() >= 4) {
                questions.clear();
            }

            out.setQuestions(questions);

            JsonNode extracted = root.path("extracted");
            if (extracted.isObject()) {
                Map<String, String> map = new HashMap<>();
                extracted.fields().forEachRemaining(e ->
                    map.put(e.getKey(), e.getValue().asText(""))
                );
                out.setExtracted(map);
            }
        } catch (Exception e) {
            log.error("Agent clarify failed: {}", e.getMessage());
            out.setConfidence(0.6);
            out.setQuestions(List.of("请再描述一下：这个 Agent 要解决什么问题、用在哪里？"));
        }

        return out;
    }

    public String chatReply(String message, List<Map<String, String>> history, String intent) {
        String historyText = formatHistory(history);
        String userPrompt = "意图: " + intent + "\n用户: " + message + "\n\n对话历史:\n" + historyText;

        if ("QUERY_AGENTS".equals(intent)) {
            return "你可以在左侧导航进入「Agent 列表」查看已有 Agent。若要新建，直接说「帮我做一个…机器人」即可。";
        }
        if ("EDIT_AGENT".equals(intent) || "DELETE_AGENT".equals(intent)) {
            return "请先到「Agent 列表」选择要操作的 Agent。若要新建，可以说「帮我做一个…机器人」。";
        }

        try {
            return llmClient.chat(CHAT_SYSTEM_PROMPT, List.of(Map.of("role", "user", "content", userPrompt)), 512);
        } catch (Exception e) {
            log.error("Chat reply failed: {}", e.getMessage());
            return "你好！我是 AgentForge。有创建 Agent 的需求时，可以说「帮我做一个售前答疑机器人」。";
        }
    }

    private IntentResult parseIntentResult(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        IntentResult result = new IntentResult();
        result.setIntent(root.path("intent").asText("UNKNOWN"));
        result.setConfidence(root.path("confidence").asDouble(0.5));
        result.setClarificationNeeded(root.path("clarification_needed").asBoolean(false));
        result.setClarificationQuestion(root.path("clarification_question").asText(""));
        return result;
    }

    private String formatHistory(List<Map<String, String>> history) {
        if (history == null || history.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> msg : history) {
            sb.append(msg.getOrDefault("role", "user"))
                .append(": ")
                .append(msg.getOrDefault("content", ""))
                .append("\n");
        }
        return sb.toString();
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
}
