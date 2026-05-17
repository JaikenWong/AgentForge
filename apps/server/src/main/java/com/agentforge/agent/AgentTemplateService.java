package com.agentforge.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class AgentTemplateService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<AgentTemplate> templates = Collections.emptyList();

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource("agent-templates.json").getInputStream()) {
            templates = objectMapper.readValue(in, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load agent-templates.json", e);
        }
    }

    public List<AgentTemplate> listTemplates() {
        return templates;
    }

    public AgentTemplate getTemplate(String templateId) {
        return findTemplate(templateId)
            .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
    }

    public Optional<AgentTemplate> findTemplate(String templateId) {
        return templates.stream()
            .filter(t -> t.getId().equals(templateId))
            .findFirst();
    }

    @Data
    public static class AgentTemplate {
        private String id;
        private String name;
        private String description;
        private String prompt;
        private String config;
    }
}
