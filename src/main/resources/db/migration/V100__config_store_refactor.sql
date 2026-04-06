-- ============================================================================
-- V100: ConfigStore 重构
-- - 新增 scope_type, scope_id, reference_type, reference_id, owner_type, owner_id 字段
-- - 新增 status, version, published_at, published_by 字段（Draft/Publish 机制）
-- - 创建 config_definitions 表存储配置项元数据和默认值
-- ============================================================================

-- 1. 重新创建 config_store 表（保留原有数据）
-- 先备份原数据到临时表
CREATE TABLE IF NOT EXISTS config_store_backup AS
SELECT id, workspace, entity_type, entity_id, config_key, config_value, created_at, updated_at
FROM config_store;

-- 删除原表
DROP TABLE IF EXISTS config_store;

-- 创建新的 config_store 表
CREATE TABLE config_store (
    id VARCHAR(255) PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL COMMENT 'GLOBAL, STUDIO, WORKSPACE, CHARACTER, MEMORY, OBSERVER, MEMBER, SKILL, TOOL, WORKSPACE_REF_OVERRIDE',
    scope_id VARCHAR(64) COMMENT '作用域 ID（如 workspaceId）',
    target_type VARCHAR(32) COMMENT '目标类型：self 或 CHARACTER/SKILL/OBSERVER/TOOL/MEMBER',
    target_id VARCHAR(64) COMMENT '目标 ID',
    config_key VARCHAR(255) NOT NULL COMMENT '配置键（不含前缀）',
    config_value JSON COMMENT '配置值',
    reference_type VARCHAR(32) COMMENT '被引用资产类型（CHARACTER, SKILL, TOOL）用于 workspace_ref_override',
    reference_id VARCHAR(64) COMMENT '被引用资产 ID',
    owner_type VARCHAR(32) COMMENT '所属者类型（CHARACTER, WORKSPACE）用于 Memory',
    owner_id VARCHAR(64) COMMENT '所属者 ID',
    status VARCHAR(20) DEFAULT 'PUBLISHED' COMMENT 'DRAFT, PUBLISHED',
    version INT DEFAULT 1 COMMENT '版本号',
    published_at DATETIME COMMENT '发布时间',
    published_by VARCHAR(100) COMMENT '发布人',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_scope (scope_type, scope_id),
    INDEX idx_config_key (config_key(64)),
    INDEX idx_target (target_type, target_id),
    INDEX idx_reference (reference_type, reference_id),
    INDEX idx_owner (owner_type, owner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 创建 config_definitions 表
CREATE TABLE IF NOT EXISTS config_definitions (
    id VARCHAR(100) PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL COMMENT '配置项所属作用域类型',
    config_key VARCHAR(128) NOT NULL COMMENT '格式: {scopeType}:{configKey}，不含 scopeId/target',
    value_type VARCHAR(20) NOT NULL COMMENT 'NUMBER, STRING, BOOLEAN, LIST, OBJECT',
    description VARCHAR(500) COMMENT '配置项描述',
    default_value JSON COMMENT 'hardcoded 默认值',
    version INT DEFAULT 1 COMMENT '版本号',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    UNIQUE KEY uk_scope_key (scope_type, config_key),
    INDEX idx_def_scope_type (scope_type),
    INDEX idx_def_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 初始化 config_definitions 数据（从打标清单中的 hardcoded 默认值）
INSERT INTO config_definitions (id, scope_type, config_key, value_type, description, default_value, version, created_at, updated_at) VALUES
-- Character 配置
('CHARACTER:maxSteps', 'CHARACTER', 'CHARACTER:maxSteps', 'NUMBER', '最大工作流步骤数', 10, 1, NOW(), NOW()),
('CHARACTER:maxIterations', 'CHARACTER', 'CHARACTER:maxIterations', 'NUMBER', 'ReAct 最大迭代次数', 10, 1, NOW(), NOW()),
('CHARACTER:enableMemorySearch', 'CHARACTER', 'CHARACTER:enableMemorySearch', 'BOOLEAN', '是否启用记忆搜索', true, 1, NOW(), NOW()),
('CHARACTER:enableToolUse', 'CHARACTER', 'CHARACTER:enableToolUse', 'BOOLEAN', '是否启用工具使用', true, 1, NOW(), NOW()),

-- Observer 配置
('OBSERVER:optimizationThreshold', 'OBSERVER', 'OBSERVER:optimizationThreshold', 'NUMBER', '优化阈值', 0.6, 1, NOW(), NOW()),
('OBSERVER:consecutiveLowScoreThreshold', 'OBSERVER', 'OBSERVER:consecutiveLowScoreThreshold', 'NUMBER', '连续低分阈值', 3, 1, NOW(), NOW()),
('OBSERVER:periodicEvaluationHours', 'OBSERVER', 'OBSERVER:periodicEvaluationHours', 'NUMBER', '定期评估小时', 24, 1, NOW(), NOW()),
('OBSERVER:manualApprovalRequired', 'OBSERVER', 'OBSERVER:manualApprovalRequired', 'BOOLEAN', '是否需要人工审批', true, 1, NOW(), NOW()),
('OBSERVER:planWindowHours', 'OBSERVER', 'OBSERVER:planWindowHours', 'NUMBER', '计划窗口小时', 24, 1, NOW(), NOW()),
('OBSERVER:maxPlanItems', 'OBSERVER', 'OBSERVER:maxPlanItems', 'NUMBER', '最大计划项', 50, 1, NOW(), NOW()),

-- Memory 配置
('MEMORY:similarityThreshold', 'MEMORY', 'MEMORY:similarityThreshold', 'NUMBER', '去重相似度阈值', 0.7, 1, NOW(), NOW()),

-- Skill 配置
('SKILL:maxSingleFileBytes', 'SKILL', 'SKILL:maxSingleFileBytes', 'NUMBER', '单文件最大字节数', 2097152, 1, NOW(), NOW()),
('SKILL:maxZipFileBytes', 'SKILL', 'SKILL:maxZipFileBytes', 'NUMBER', 'ZIP 文件最大字节数', 10485760, 1, NOW(), NOW()),
('SKILL:maxUnzipTotalBytes', 'SKILL', 'SKILL:maxUnzipTotalBytes', 'NUMBER', '解压总量最大字节数', 52428800, 1, NOW(), NOW()),
('SKILL:maxFileCount', 'SKILL', 'SKILL:maxFileCount', 'NUMBER', '最大文件数', 100, 1, NOW(), NOW()),
('SKILL:askTimeoutMs', 'SKILL', 'SKILL:askTimeoutMs', 'NUMBER', '权限询问超时（毫秒）', 30000, 1, NOW(), NOW()),

-- Schedule 配置
('SCHEDULE:defaultSeconds', 'SCHEDULE', 'SCHEDULE:defaultSeconds', 'NUMBER', 'Cron 表达式默认秒数', 0, 1, NOW(), NOW()),

-- Workflow/Evaluation 配置
('WORKFLOW:maxIterations', 'WORKFLOW', 'WORKFLOW:maxIterations', 'NUMBER', '工作流最大迭代次数', 10, 1, NOW(), NOW()),
('EVALUATION:maxDurationMs', 'EVALUATION', 'EVALUATION:maxDurationMs', 'NUMBER', '评估引擎最大耗时（毫秒）', 100000, 1, NOW(), NOW()),
('EVALUATION:maxTokens', 'EVALUATION', 'EVALUATION:maxTokens', 'NUMBER', '评估引擎最大 Token', 100000, 1, NOW(), NOW()),

-- Bash/Exec 配置
('BASH:defaultJobTtlMs', 'BASH', 'BASH:defaultJobTtlMs', 'NUMBER', '默认任务 TTL（毫秒）', 1800000, 1, NOW(), NOW()),
('BASH:minJobTtlMs', 'BASH', 'BASH:minJobTtlMs', 'NUMBER', '最小任务 TTL（毫秒）', 60000, 1, NOW(), NOW()),
('BASH:maxJobTtlMs', 'BASH', 'BASH:maxJobTtlMs', 'NUMBER', '最大任务 TTL（毫秒）', 10800000, 1, NOW(), NOW()),
('BASH:defaultMaxOutputChars', 'BASH', 'BASH:defaultMaxOutputChars', 'NUMBER', '默认最大输出字符', 50000, 1, NOW(), NOW()),
('BASH:defaultTailChars', 'BASH', 'BASH:defaultTailChars', 'NUMBER', '默认尾部字符数', 2000, 1, NOW(), NOW()),

-- Web 配置
('WEB:maxBodyChars', 'WEB', 'WEB:maxBodyChars', 'NUMBER', 'Web 获取最大字符数', 50000, 1, NOW(), NOW()),
('WEB:requestTimeout', 'WEB', 'WEB:requestTimeout', 'NUMBER', 'Web 获取请求超时（秒）', 30, 1, NOW(), NOW()),
('WEB:searchTimeout', 'WEB', 'WEB:searchTimeout', 'NUMBER', 'Web 搜索超时（秒）', 15, 1, NOW(), NOW()),

-- Exec 配置
('EXEC:defaultTimeoutSeconds', 'EXEC', 'EXEC:defaultTimeoutSeconds', 'NUMBER', '执行默认超时（秒）', 120, 1, NOW(), NOW()),
('EXEC:maxOutputLength', 'EXEC', 'EXEC:maxOutputLength', 'NUMBER', '执行最大输出长度', 50000, 1, NOW(), NOW()),

-- FileTools 配置
('FILETOOLS:maxMatches', 'FILETOOLS', 'FILETOOLS:maxMatches', 'NUMBER', '文件搜索最大匹配数', 50, 1, NOW(), NOW()),

-- ToolDisplay 配置
('TOOL:maxDetailEntries', 'TOOL', 'TOOL:maxDetailEntries', 'NUMBER', '最大详情条目', 8, 1, NOW(), NOW()),

-- Sandbox 配置
('SANDBOX:workspaceRoot', 'SANDBOX', 'SANDBOX:workspaceRoot', 'STRING', 'Sandbox 工作空间根目录', '~/.dragon/sandboxes', 1, NOW(), NOW()),
('SANDBOX:dockerImage', 'SANDBOX', 'SANDBOX:dockerImage', 'STRING', 'Docker 镜像', 'dragonhead-sandbox', 1, NOW(), NOW()),
('SANDBOX:containerPrefix', 'SANDBOX', 'SANDBOX:containerPrefix', 'STRING', '容器名称前缀', 'dragon-sbx-', 1, NOW(), NOW()),
('SANDBOX:idleHours', 'SANDBOX', 'SANDBOX:idleHours', 'NUMBER', '空闲容器清理阈值（小时）', 24, 1, NOW(), NOW()),
('SANDBOX:maxAgeDays', 'SANDBOX', 'SANDBOX:maxAgeDays', 'NUMBER', '容器最大保留天数', 7, 1, NOW(), NOW()),
('SANDBOX:cdpPort', 'SANDBOX', 'SANDBOX:cdpPort', 'NUMBER', 'CDP 端口', 9222, 1, NOW(), NOW()),
('SANDBOX:vncPort', 'SANDBOX', 'SANDBOX:vncPort', 'NUMBER', 'VNC 端口', 5900, 1, NOW(), NOW()),
('SANDBOX:novncPort', 'SANDBOX', 'SANDBOX:novncPort', 'NUMBER', 'noVNC 端口', 6080, 1, NOW(), NOW()),

-- User/SMS 配置
('USER:maxLoginFailCount', 'USER', 'USER:maxLoginFailCount', 'NUMBER', '最大登录失败次数', 5, 1, NOW(), NOW()),
('USER:lockMinutes', 'USER', 'USER:lockMinutes', 'NUMBER', '账户锁定分钟数', 15, 1, NOW(), NOW()),
('SMS:codeValidityMinutes', 'SMS', 'SMS:codeValidityMinutes', 'NUMBER', '验证码有效期（分钟）', 5, 1, NOW(), NOW()),
('SMS:sendCooldownSeconds', 'SMS', 'SMS:sendCooldownSeconds', 'NUMBER', '验证码发送冷却（秒）', 60, 1, NOW(), NOW()),

-- Workspace Personality 配置
('WORKSPACE:workingStyle', 'WORKSPACE', 'WORKSPACE:workingStyle', 'STRING', '工作风格', 'COLLABORATIVE', 1, NOW(), NOW()),
('WORKSPACE:decisionPattern', 'WORKSPACE', 'WORKSPACE:decisionPattern', 'STRING', '决策模式', 'CONSULTATIVE', 1, NOW(), NOW()),
('WORKSPACE:riskTolerance', 'WORKSPACE', 'WORKSPACE:riskTolerance', 'NUMBER', '风险容忍度', 0.5, 1, NOW(), NOW()),

-- Registry 默认值
('REGISTRY:defaultCharacterId', 'REGISTRY', 'REGISTRY:defaultCharacterId', 'STRING', '默认 Character ID', null, 1, NOW(), NOW()),
('REGISTRY:defaultModelId', 'REGISTRY', 'REGISTRY:defaultModelId', 'STRING', '默认 Model ID', null, 1, NOW(), NOW()),
('REGISTRY:defaultObserverId', 'REGISTRY', 'REGISTRY:defaultObserverId', 'STRING', '默认 Observer ID', null, 1, NOW(), NOW());

-- 4. 清理临时表
DROP TABLE IF EXISTS config_store_backup;
