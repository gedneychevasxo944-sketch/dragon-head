package org.dragon.sandbox.domain;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Workspace Sandbox 实例。
 * 代表一个 workspace 的完整执行环境。
 *
 * @since 1.0
 */
@Data
@Builder
public class Sandbox {

    /** 归属的 workspace ID */
    private Long workspaceId;

    /** sandbox 根目录 */
    private Path rootDir;

    /** skills 目录（存放 Skill 脚本文件） */
    private Path skillsDir;

    /** workspace 持久化工作目录 */
    private Path workspaceDir;

    /** 临时执行目录根路径 */
    private Path tmpDir;

    /**
     * sandbox 级别的环境变量。
     * 包含：Skill 声明的环境变量 + workspace 配置的变量。
     * 所有在此 sandbox 中执行的命令都会继承这些变量。
     */
    private Map<String, String> environmentVariables;

    /** sandbox 创建时间 */
    private LocalDateTime createdAt;

    /** sandbox 当前状态 */
    private SandboxState state;
}