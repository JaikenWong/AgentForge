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

        if ("CREATE_AGENT".equals(intent.getIntent())) {
            ClarifyResult result = intentService.clarifyForCreateAgent(request.getMessage(), history);
            return ResponseEntity.ok(result);
        }

        ClarifyResult out = new ClarifyResult();
        out.setIntent(intent.getIntent());
        out.setGoal("");
        out.setConfidence(intent.getConfidence());
        out.setExtracted(new HashMap<>());
        out.setQuestions(List.of());
        out.setReply(intentService.chatReply(request.getMessage(), history, intent.getIntent()));
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
        private String intent;
        private String goal;
        private double confidence;
        private List<String> questions;
        private Map<String, String> extracted;
        /** 非 CREATE_AGENT 时的对话回复 */
        private String reply;
    }
}
