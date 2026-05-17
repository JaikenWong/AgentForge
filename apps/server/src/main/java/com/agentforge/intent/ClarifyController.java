package com.agentforge.intent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intent")
@RequiredArgsConstructor
public class ClarifyController {

    private final IntentService intentService;

    @PostMapping("/analyze")
    public ResponseEntity<IntentService.IntentResult> analyzeIntent(
            @RequestBody ClarifyRequest request) {
        String conversationHistory = buildConversationHistory(request.getMessages());
        IntentService.IntentResult result = intentService.analyzeIntent(request.getUserInput(), conversationHistory);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/clarify")
    public ResponseEntity<ClarifyResult> clarify(@RequestBody ClarifyMessage request) {
        List<Map<String, String>> history = request.getHistory() != null ? request.getHistory() : new ArrayList<>();
        String conversationHistory = buildConversationHistory(history);
        IntentService.IntentResult intent = intentService.analyzeIntent(request.getMessage(), conversationHistory);

        ClarifyResult out = new ClarifyResult();
        out.setGoal(request.getMessage());
        out.setConfidence(intent.getConfidence());
        out.setExtracted(new HashMap<>());

        List<String> questions = new ArrayList<>();
        if (intent.isClarificationNeeded() && intent.getClarificationQuestion() != null) {
            questions.add(intent.getClarificationQuestion());
        } else if (intent.getConfidence() < 0.7) {
            questions.add("请补充更多细节：目标用户、使用场景、希望接入的渠道？");
        }
        out.setQuestions(questions);
        return ResponseEntity.ok(out);
    }

    private String buildConversationHistory(List<Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            return "无历史对话";
        }

        StringBuilder history = new StringBuilder();
        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "unknown");
            String content = msg.getOrDefault("content", "");
            history.append(role).append(": ").append(content).append("\n");
        }
        return history.toString();
    }

    @Data
    public static class ClarifyRequest {
        private String userInput;
        private List<Map<String, String>> messages;
    }

    @Data
    public static class ClarifyMessage {
        private String message;
        private List<Map<String, String>> history;
    }

    @Data
    public static class ClarifyResult {
        private String goal;
        private double confidence;
        private List<String> questions;
        private Map<String, String> extracted;
    }
}
