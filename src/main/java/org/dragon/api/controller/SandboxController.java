package org.dragon.api.controller;

import org.dragon.sandbox.domain.ExecutionRequest;
import org.dragon.sandbox.domain.ExecutionResult;
import org.dragon.sandbox.executor.SandboxExecutor;
import org.dragon.sandbox.manager.SandboxManager;
import org.dragon.sandbox.service.SandboxEnvConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Sandbox 管理接口。
 *
 * POST /api/sandbox/{workspaceId}/init          - 初始化 sandbox
 * POST /api/sandbox/{workspaceId}/execute        - 执行命令（主要供 Agent 内部调用）
 * GET  /api/sandbox/{workspaceId}/env            - 查看环境变量（脱敏）
 * POST /api/sandbox/{workspaceId}/env            - 追加环境变量
 * DELETE /api/sandbox/{workspaceId}/env          - 移除环境变量
 * POST /api/sandbox/{workspaceId}/skills/sync    - 手动触发 Skill 文件同步
 * DELETE /api/sandbox/{workspaceId}              - 销毁 sandbox
 *
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final SandboxManager sandboxManager;
    private final SandboxExecutor sandboxExecutor;
    private final SandboxEnvConfigService envConfigService;

    /**
     * 初始化指定 workspace 的 sandbox。
     * 幂等操作：若已存在则直接返回。
     */
    @PostMapping("/{workspaceId}/init")
    public ResponseEntity<Void> initSandbox(@PathVariable Long workspaceId) {
        sandboxManager.getOrCreate(workspaceId);
        return ResponseEntity.ok().build();
    }

    /**
     * 在 sandbox 中执行命令。
     * 主要供 Agent 的 Tool Call 处理器内部调用，也可用于调试。
     */
    @PostMapping("/{workspaceId}/execute")
    public ResponseEntity<ExecutionResult> execute(
            @PathVariable Long workspaceId,
            @RequestBody ExecutionRequest request) {
        request.setWorkspaceId(workspaceId);
        if (request.getExecutionId() == null) {
            request.setExecutionId(UUID.randomUUID().toString());
        }
        ExecutionResult result = sandboxExecutor.execute(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 查看 sandbox 当前环境变量（脱敏）。
     */
    @GetMapping("/{workspaceId}/env")
    public ResponseEntity<Map<String, String>> getEnvVars(@PathVariable Long workspaceId) {
        return ResponseEntity.ok(envConfigService.getMaskedEnvVars(workspaceId));
    }

    /**
     * 追加环境变量到 sandbox。
     */
    @PostMapping("/{workspaceId}/env")
    public ResponseEntity<Void> appendEnvVars(
            @PathVariable Long workspaceId,
            @RequestBody Map<String, String> envVars) {
        envConfigService.appendEnvVars(workspaceId, envVars);
        return ResponseEntity.ok().build();
    }

    /**
     * 从 sandbox 移除环境变量。
     */
    @DeleteMapping("/{workspaceId}/env")
    public ResponseEntity<Void> removeEnvVars(
            @PathVariable Long workspaceId,
            @RequestBody java.util.List<String> envKeys) {
        envConfigService.removeEnvVars(workspaceId, envKeys);
        return ResponseEntity.ok().build();
    }

    /**
     * 手动触发 sandbox 中 Skill 文件的全量同步。
     */
    @PostMapping("/{workspaceId}/skills/sync")
    public ResponseEntity<Void> syncSkills(@PathVariable Long workspaceId) {
        sandboxManager.get(workspaceId).ifPresent(sandbox ->
                sandboxManager.syncSkillFiles(workspaceId, sandbox.getSkillsDir()));
        return ResponseEntity.ok().build();
    }

    /**
     * 销毁 sandbox（清理文件和内存）。
     */
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> destroySandbox(@PathVariable Long workspaceId) {
        sandboxManager.destroy(workspaceId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取 sandbox 状态信息。
     */
    @GetMapping("/{workspaceId}")
    public ResponseEntity<Map<String, Object>> getSandboxStatus(@PathVariable Long workspaceId) {
        return sandboxManager.get(workspaceId)
                .map(sandbox -> {
                    Map<String, Object> status = Map.of(
                            "workspaceId", sandbox.getWorkspaceId(),
                            "rootDir", sandbox.getRootDir().toString(),
                            "state", sandbox.getState(),
                            "createdAt", sandbox.getCreatedAt().toString(),
                            "activeCount", sandboxManager.activeSandboxCount()
                    );
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}