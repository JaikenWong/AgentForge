package com.agentforge.generator;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class GeneratorController {

    private final GeneratorService generatorService;

    @PostMapping
    public ResponseEntity<GeneratorService.GeneratorResult> generateAgent(
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody GenerateRequest request) {
        GeneratorService.GeneratorResult result = generatorService.generateAgent(
            userId, tenantId, request.getPrompt()
        );
        return ResponseEntity.ok(result);
    }

    @Data
    public static class GenerateRequest {
        private String prompt;
    }
}
