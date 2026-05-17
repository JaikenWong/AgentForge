package com.agentforge.runtime;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentDeployController {

    private final FeishuDeployService feishuDeployService;

    @GetMapping("/{id}/deployments")
    public ResponseEntity<List<FeishuDeployService.DeploymentDto>> listDeployments(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(feishuDeployService.listDeployments(id, tenantId));
    }

    @PostMapping("/{id}/deploy/feishu/bind-code")
    public ResponseEntity<FeishuDeployService.BindCodeDto> createFeishuBindCode(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId) {
        return ResponseEntity.ok(feishuDeployService.createBindCode(id, tenantId));
    }

    @PostMapping("/{id}/deploy/feishu")
    public ResponseEntity<FeishuDeployService.DeploymentDto> deployFeishu(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody DeployFeishuRequest request) {
        return ResponseEntity.ok(
            feishuDeployService.deploy(id, tenantId, request.getChatId())
        );
    }

    @DeleteMapping("/{id}/deploy/feishu")
    public ResponseEntity<Void> undeployFeishu(
            @PathVariable Long id,
            @RequestAttribute("tenantId") Long tenantId,
            @RequestParam String chatId) {
        feishuDeployService.undeploy(id, tenantId, chatId);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class DeployFeishuRequest {
        private String chatId;
    }
}
