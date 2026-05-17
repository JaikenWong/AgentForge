package com.agentforge.agent;

import com.agentforge.llm.LlmClient;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AgentRunService {

    private final AgentMapper agentMapper;
    private final AgentExecutionLogMapper executionLogMapper;
    private final LlmClient llmClient;

    @Transactional(readOnly = true)
    public List<ExecutionLogDto> getLogs(Long agentId, Long tenantId, int limit) {
        Agent agent = requireAgent(agentId, tenantId);
        int cap = Math.min(Math.max(limit, 1), 100);

        return executionLogMapper.selectList(
                Wrappers.<AgentExecutionLog>lambdaQuery()
                    .eq(AgentExecutionLog::getAgentId, agent.getId())
                    .orderByDesc(AgentExecutionLog::getCreatedAt)
                    .last("LIMIT " + cap)
            )
            .stream()
            .map(ExecutionLogDto::fromEntity)
            .toList();
    }

    public ChatResult chat(Long agentId, Long userId, Long tenantId, String channel, ChatRequest request) {
        Agent agent = requireRunnableAgent(agentId, tenantId);
        long start = System.currentTimeMillis();

        List<Map<String, String>> messages = buildMessages(request);
        AgentExecutionLog logRow = new AgentExecutionLog();
        logRow.setAgentId(agent.getId());
        logRow.setUserId(userId);
        logRow.setTenantId(tenantId);
        logRow.setChannel(channel != null ? channel : "web");
        logRow.setInputText(request.getMessage());
        logRow.setCreatedAt(LocalDateTime.now());

        try {
            String reply = llmClient.chat(agent.getPrompt(), messages, 1024);
            logRow.setOutputText(reply);
            logRow.setStatus("success");
            logRow.setLatencyMs((int) (System.currentTimeMillis() - start));
            executionLogMapper.insert(logRow);

            ChatResult result = new ChatResult();
            result.setReply(reply);
            result.setLogId(logRow.getId());
            result.setLatencyMs(logRow.getLatencyMs());
            return result;
        } catch (Exception e) {
            log.error("Agent chat failed: agentId={} {}", agentId, e.getMessage());
            logRow.setStatus("error");
            logRow.setErrorMessage(e.getMessage());
            logRow.setLatencyMs((int) (System.currentTimeMillis() - start));
            executionLogMapper.insert(logRow);
            throw new RuntimeException("Agent 执行失败: " + e.getMessage());
        }
    }

    private List<Map<String, String>> buildMessages(ChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getHistory() != null) {
            for (ChatMessage m : request.getHistory()) {
                if (m.getRole() == null || m.getContent() == null) continue;
                messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
            }
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));
        return messages;
    }

    private Agent requireAgent(Long agentId, Long tenantId) {
        Agent agent = agentMapper.selectById(agentId);
        if (agent == null || !agent.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Agent not found");
        }
        return agent;
    }

    private Agent requireRunnableAgent(Long agentId, Long tenantId) {
        Agent agent = requireAgent(agentId, tenantId);
        if ("archived".equals(agent.getStatus())) {
            throw new RuntimeException("Agent 已归档，请先复活后再使用");
        }
        return agent;
    }

    @Data
    public static class ChatRequest {
        private String message;
        private List<ChatMessage> history;
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }

    @Data
    public static class ChatResult {
        private String reply;
        private Long logId;
        private Integer latencyMs;
    }

    @Data
    public static class ExecutionLogDto {
        private Long id;
        private String channel;
        private String inputText;
        private String outputText;
        private String status;
        private Integer latencyMs;
        private String errorMessage;
        private LocalDateTime createdAt;

        public static ExecutionLogDto fromEntity(AgentExecutionLog log) {
            ExecutionLogDto dto = new ExecutionLogDto();
            dto.setId(log.getId());
            dto.setChannel(log.getChannel());
            dto.setInputText(log.getInputText());
            dto.setOutputText(log.getOutputText());
            dto.setStatus(log.getStatus());
            dto.setLatencyMs(log.getLatencyMs());
            dto.setErrorMessage(log.getErrorMessage());
            dto.setCreatedAt(log.getCreatedAt());
            return dto;
        }
    }
}
