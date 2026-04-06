package org.dragon.sandbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Sandbox 环境变量配置服务。
 *
 * 职责：
 * 管理 workspace 级别的自定义环境变量配置。
 * 这些变量会在 sandbox 创建时注入，供 Skill 脚本使用。
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxEnvConfigService {

    // private final org.dragon.sandbox.manager.SandboxManager sandboxManager;

    // /**
    //  * 为指定 workspace 的 sandbox 动态追加环境变量。
    //  * 若 sandbox 已存在，立即生效（更新内存中的 sandbox 实例）。
    //  *
    //  * @param workspaceId workspace ID
    //  * @param envVars     要追加的环境变量
    //  */
    // public void appendEnvVars(Long workspaceId, Map<String, String> envVars) {
    //     sandboxManager.get(workspaceId).ifPresent(sandbox -> {
    //         sandbox.getEnvironmentVariables().putAll(envVars);
    //         log.info("Sandbox 环境变量已更新: workspaceId={}, keys={}",
    //                 workspaceId, envVars.keySet());
    //     });
    // }

    // /**
    //  * 移除指定 workspace sandbox 中的环境变量。
    //  *
    //  * @param workspaceId workspace ID
    //  * @param envKeys     要移除的变量名列表
    //  */
    // public void removeEnvVars(Long workspaceId, java.util.List<String> envKeys) {
    //     sandboxManager.get(workspaceId).ifPresent(sandbox -> {
    //         envKeys.forEach(key -> sandbox.getEnvironmentVariables().remove(key));
    //         log.info("Sandbox 环境变量已移除: workspaceId={}, keys={}", workspaceId, envKeys);
    //     });
    // }

    // /**
    //  * 获取指定 workspace sandbox 当前的所有环境变量（脱敏）。
    //  * 用于管理界面展示，敏感值替换为 "******"。
    //  */
    // public Map<String, String> getMaskedEnvVars(Long workspaceId) {
    //     return sandboxManager.get(workspaceId)
    //             .map(sandbox -> maskSensitiveValues(sandbox.getEnvironmentVariables()))
    //             .orElse(Map.of());
    // }

    // /**
    //  * 对敏感环境变量的值进行脱敏处理。
    //  * 包含 KEY、SECRET、TOKEN、PASSWORD 关键字的变量名视为敏感变量。
    //  */
    // private Map<String, String> maskSensitiveValues(Map<String, String> envVars) {
    //     Map<String, String> masked = new java.util.LinkedHashMap<>();
    //     envVars.forEach((k, v) -> {
    //         String upperKey = k.toUpperCase();
    //         boolean isSensitive = upperKey.contains("KEY")
    //                 || upperKey.contains("SECRET")
    //                 || upperKey.contains("TOKEN")
    //                 || upperKey.contains("PASSWORD");
    //         masked.put(k, isSensitive ? "******" : v);
    //     });
    //     return masked;
    // }
}