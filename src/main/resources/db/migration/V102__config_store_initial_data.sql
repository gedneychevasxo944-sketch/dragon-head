-- V102: 冷启动配置数据初始化
-- 将默认 prompt 和配置项写入 config_store 表

-- ID 格式: {scopeBit}:{workspaceId}:{characterId}:{toolId}:{skillId}:{memoryId}:{configKey}
-- GLOBAL_WORKSPACE level (scopeBit=5), 所有层级 ID 为空

-- ==================== 系统配置项默认值 ====================
-- 从旧 config_definitions 表迁移

-- Character 配置 (scopeBit=9, GLOBAL -> CHARACTER)
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('9:::::maxSteps', 9, NULL, NULL, NULL, NULL, NULL, 'maxSteps', 10, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('9:::::maxIterations', 9, NULL, NULL, NULL, NULL, NULL, 'maxIterations', 10, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('9:::::enableMemorySearch', 9, NULL, NULL, NULL, NULL, NULL, 'enableMemorySearch', true, 'BOOLEAN', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('9:::::enableToolUse', 9, NULL, NULL, NULL, NULL, NULL, 'enableToolUse', true, 'BOOLEAN', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Skill 配置 (scopeBit=17, GLOBAL -> SKILL)
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('17:::::maxSingleFileBytes', 17, NULL, NULL, NULL, NULL, NULL, 'maxSingleFileBytes', 2097152, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('17:::::maxZipFileBytes', 17, NULL, NULL, NULL, NULL, NULL, 'maxZipFileBytes', 10485760, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('17:::::maxUnzipTotalBytes', 17, NULL, NULL, NULL, NULL, NULL, 'maxUnzipTotalBytes', 52428800, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('17:::::maxFileCount', 17, NULL, NULL, NULL, NULL, NULL, 'maxFileCount', 100, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('17:::::askTimeoutMs', 17, NULL, NULL, NULL, NULL, NULL, 'askTimeoutMs', 30000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Memory 配置 (scopeBit=65, GLOBAL -> MEMORY)
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('65:::::similarityThreshold', 65, NULL, NULL, NULL, NULL, NULL, 'similarityThreshold', 0.7, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- 系统级配置 (scopeBit=5, GLOBAL -> WORKSPACE)
-- 包括: schedule, workflow, evaluation, bash, web, exec, filetools, tool, sandbox, user, sms, workspace, registry

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::schedule.defaultSeconds', 5, NULL, NULL, NULL, NULL, NULL, 'schedule.defaultSeconds', 0, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::workflow.maxIterations', 5, NULL, NULL, NULL, NULL, NULL, 'workflow.maxIterations', 10, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::evaluation.maxDurationMs', 5, NULL, NULL, NULL, NULL, NULL, 'evaluation.maxDurationMs', 100000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::evaluation.maxTokens', 5, NULL, NULL, NULL, NULL, NULL, 'evaluation.maxTokens', 100000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::bash.defaultJobTtlMs', 5, NULL, NULL, NULL, NULL, NULL, 'bash.defaultJobTtlMs', 1800000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::bash.minJobTtlMs', 5, NULL, NULL, NULL, NULL, NULL, 'bash.minJobTtlMs', 60000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::bash.maxJobTtlMs', 5, NULL, NULL, NULL, NULL, NULL, 'bash.maxJobTtlMs', 10800000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::bash.defaultMaxOutputChars', 5, NULL, NULL, NULL, NULL, NULL, 'bash.defaultMaxOutputChars', 50000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::bash.defaultTailChars', 5, NULL, NULL, NULL, NULL, NULL, 'bash.defaultTailChars', 2000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::web.maxBodyChars', 5, NULL, NULL, NULL, NULL, NULL, 'web.maxBodyChars', 50000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::web.requestTimeout', 5, NULL, NULL, NULL, NULL, NULL, 'web.requestTimeout', 30, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::web.searchTimeout', 5, NULL, NULL, NULL, NULL, NULL, 'web.searchTimeout', 15, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::exec.defaultTimeoutSeconds', 5, NULL, NULL, NULL, NULL, NULL, 'exec.defaultTimeoutSeconds', 120, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::exec.maxOutputLength', 5, NULL, NULL, NULL, NULL, NULL, 'exec.maxOutputLength', 50000, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::filetools.maxMatches', 5, NULL, NULL, NULL, NULL, NULL, 'filetools.maxMatches', 50, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::tool.maxDetailEntries', 5, NULL, NULL, NULL, NULL, NULL, 'tool.maxDetailEntries', 8, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.workspaceRoot', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.workspaceRoot', '~/.dragon/sandboxes', 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.dockerImage', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.dockerImage', 'dragonhead-sandbox', 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.containerPrefix', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.containerPrefix', 'dragon-sbx-', 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.idleHours', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.idleHours', 24, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.maxAgeDays', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.maxAgeDays', 7, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.cdpPort', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.cdpPort', 9222, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.vncPort', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.vncPort', 5900, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sandbox.novncPort', 5, NULL, NULL, NULL, NULL, NULL, 'sandbox.novncPort', 6080, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::user.maxLoginFailCount', 5, NULL, NULL, NULL, NULL, NULL, 'user.maxLoginFailCount', 5, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::user.lockMinutes', 5, NULL, NULL, NULL, NULL, NULL, 'user.lockMinutes', 15, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sms.codeValidityMinutes', 5, NULL, NULL, NULL, NULL, NULL, 'sms.codeValidityMinutes', 5, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::sms.sendCooldownSeconds', 5, NULL, NULL, NULL, NULL, NULL, 'sms.sendCooldownSeconds', 60, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::workspace.workingStyle', 5, NULL, NULL, NULL, NULL, NULL, 'workspace.workingStyle', 'COLLABORATIVE', 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::workspace.decisionPattern', 5, NULL, NULL, NULL, NULL, NULL, 'workspace.decisionPattern', 'CONSULTATIVE', 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::workspace.riskTolerance', 5, NULL, NULL, NULL, NULL, NULL, 'workspace.riskTolerance', 0.5, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::registry.defaultCharacterId', 5, NULL, NULL, NULL, NULL, NULL, 'registry.defaultCharacterId', NULL, 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::registry.defaultModelId', 5, NULL, NULL, NULL, NULL, NULL, 'registry.defaultModelId', NULL, 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::registry.defaultObserverId', 5, NULL, NULL, NULL, NULL, NULL, 'registry.defaultObserverId', NULL, 'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Observer 配置 (scopeBit=5, GLOBAL_WORKSPACE - OBSERVER 作为功能类别，不是继承层级)
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::observer.optimizationThreshold', 5, NULL, NULL, NULL, NULL, NULL, 'observer.optimizationThreshold', 0.6, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::observer.consecutiveLowScoreThreshold', 5, NULL, NULL, NULL, NULL, NULL, 'observer.consecutiveLowScoreThreshold', 3, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::observer.periodicEvaluationHours', 5, NULL, NULL, NULL, NULL, NULL, 'observer.periodicEvaluationHours', 24, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::observer.manualApprovalRequired', 5, NULL, NULL, NULL, NULL, NULL, 'observer.manualApprovalRequired', true, 'BOOLEAN', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::observer.planWindowHours', 5, NULL, NULL, NULL, NULL, NULL, 'observer.planWindowHours', 24, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::observer.maxPlanItems', 5, NULL, NULL, NULL, NULL, NULL, 'observer.maxPlanItems', 50, 'NUMBER', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Observer 模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/observer.suggestion', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/observer.suggestion',
'# Observer LLM Optimization Suggestion Prompt

## Role
你是一个专业的AI优化顾问，负责分析Character或Organization在任务执行过程中的表现，并提供具体、可执行的优化建议。

## Input Format
```
## 目标信息
目标类型: {CHARACTER | ORGANIZATION}
目标ID: {target_id}

## 当前状态
{current_state_json}

## 最近任务执行数据
共 {n} 个任务:
任务 1:
  - 任务ID: {task_id}
  - 输入: {task_input}
  - 输出: {task_output}
  - 成功: {true | false}
  - 耗时: {duration_ms} ms
  - 错误: {error_message}

...

## 历史评估记录
评估 1:
  - 评分: {score}
  - 建议: {suggestions}
```

## Analysis Guidelines

### For Character Optimization:
1. **Personality 调整**: 分析任务执行中的沟通风格、决策方式是否合适
2. **Tag 更新**: 基于协作经历，更新对其他Character的印象和信任度
3. **技能增删**: 识别任务中暴露的技能短板或冗余
4. **沟通方式优化**: 分析消息交互模式，提出改进建议

### For Organization Optimization:
1. **WorkingStyle 调整**: 根据团队整体表现，调整工作风格倾向
2. **RiskTolerance 调整**: 分析决策风险偏好是否合适
3. **DecisionPattern 调整**: 评估决策模式的有效性
4. **CollaborationPreference 调整**: 优化协作方式

## Output Format
请以JSON数组格式输出，每条建议是一个字符串：
```json
["建议1的具体内容", "建议2的具体内容", "建议3的具体内容"]
```

## Constraints
- 生成3-5条优化建议
- 每条建议应该具体、可执行
- 建议应基于实际任务数据和评估记录
- 避免过于泛化的建议，应针对具体问题',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/observer.personalityEnhancement', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/observer.personalityEnhancement',
'# Observer LLM Personality Enhancement Prompt

## Role
你是一个专业的AI心理模型优化专家，负责根据优化建议更新Character或Organization的性格描述。

## Input Format
```
## 当前 Personality
{current_personality_json}

## 优化建议列表
1. {suggestion_1}
2. {suggestion_2}
3. {suggestion_3}
```

## Character Personality Fields
- name: 角色名称
- traits: 性格特征列表
- values: 价值观列表
- communicationStyle: 沟通风格
- decisionStyle: 决策风格
- expertise: 专业领域
- relationships: 与其他角色的关系

## Organization Personality Fields
- workingStyle: 工作风格 (AGGRESSIVE, CONSERVATIVE, COLLABORATIVE, INNOVATIVE, ANALYTICAL)
- decisionPattern: 决策模式 (DEMOCRATIC, AUTOCRATIC, CONSENSUS, CONSULTATIVE)
- riskTolerance: 风险偏好 (0-1)
- collaborationPreference: 协作偏好
- coreValues: 核心价值观
- behaviorGuidelines: 行为准则

## Output Format
请输出更新后的JSON格式的Personality描述，只包含需要修改的字段。

```json
{
  "communicationStyle": "new_style",
  "decisionStyle": "new_decision_style",
  ...
}
```

## Constraints
- 只输出需要修改的字段
- 保持其他字段不变
- 确保修改符合常识
- 建议应该具体可执行',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== ReAct 模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/react.taskDecompose', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/react.taskDecompose',
'你是一个组织调度专家，负责把复杂任务拆解为可执行的子任务。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/react.execute', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/react.execute',
'你是 ReAct 执行阶段的 PromptWriter，需要基于给定的模板、任务上下文、历史思考、动作、观察结果和工具信息，生成当前这一轮真正给执行模型使用的提示词。
生成要求：
1. 提示词必须明确用户目标、当前进度以及最新观察结果。
2. 要先引导执行模型判断"最新观察结果是否可用，是否已经足以完成任务"。
3. 如果最新观察结果已经足够完成任务，提示模型优先输出 RESPOND 或 FINISH，并在 response 字段里给出结果。
4. 如果最新观察结果不可用、为空或表现为错误，提示模型不要结束，而是重新规划 TOOL 或 MEMORY 动作。
5. 提示词中必须包含 JSON 输出约束，字段至少包含 action、tool、params、response。
6. 你只输出最终拼装好的提示词，不要解释你的拼装过程，不要输出 Markdown 代码块。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Character 模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/character.system', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/character.system',
'你是一个专业的AI数字员工，有自己的性格特点和价值观。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/character.task', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/character.task',
'请根据要求完成以下任务：',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== HR 模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/hr.hire.decision', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.hire.decision',
'请评估是否应该雇佣以下 Character 到工作空间：

Character 名称: %s
Character 描述: %s

请返回以下格式的决策：
- APPROVE：批准雇佣
- DENY：拒绝雇佣
- 需要更多信息

如果批准，请简要说明理由。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/hr.hire.select', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.hire.select',
'请从以下候选 Character 中选择一个最合适雇佣的：',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/hr.fire.decision', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.fire.decision',
'请评估是否应该解雇工作空间中的以下 Character：

Character ID: %s

请返回以下格式的决策：
- APPROVE：批准解雇
- DENY：拒绝解雇
- 需要更多信息

如果批准，请简要说明理由。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/hr.duty.generate', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.duty.generate',
'请为以下 Character 生成一个合适的职责描述：

Character 名称: %s
Character 描述: %s

请用 1-2 句话简洁描述该 Character 在工作空间中的职责。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== 选择模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/selection.generic', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/selection.generic',
'请从以下候选中选择一个最合适的：',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== MemberSelector 模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/memberSelector.select', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/memberSelector.select',
'# MemberSelector 选择成员 Prompt

## Role
你是一个专业的成员选择专家，负责从Workspace中已雇佣的Character中选择最合适的执行者来完成特定任务。

## Input Format
```
## 任务信息
任务ID: {task_id}
任务名称: {task_name}
任务描述: {task_description}
任务输入: {task_input}

## 候选成员列表
{member_list}

## 选择标准
- 技能匹配度
- 历史任务成功率
- 当前工作负载
- 协作兼容性
```

## Task
基于上述任务信息和候选成员列表，请分析每个成员的优势和劣势，然后选择最合适的成员来执行该任务。

## Output Format
请以JSON格式输出选择结果：
```json
{
  "selectedMembers": [
    {
      "characterId": "成员ID",
      "reason": "选择理由",
      "confidence": 0.95
    }
  ],
  "reasoning": "整体选择逻辑说明"
}
```

## Constraints
- 根据任务需求评估每个成员的能力匹配度
- 考虑成员的历史表现和工作负载
- 如果没有合适的成员，返回空数组
- 每个选择的置信度应为0-1之间的数值',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== ProjectManager 模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/projectManager.decompose', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/projectManager.decompose',
'# ProjectManager 任务拆解 Prompt

## Role
你是一个专业的项目经理，负责将复杂任务拆解为可执行的子任务。

## Input Format
```
## 任务信息
任务ID: {task_id}
任务名称: {task_name}
任务描述: {task_description}
任务输入: {task_input}

## 可用成员列表
{member_list}

## 工作空间信息
工作空间ID: {workspace_id}
```

## Task
请将上述任务拆解为多个可并行或顺序执行的子任务。每个子任务应该：
1. 有明确的职责描述
2. 指定合适的执行角色
3. 能够独立完成
4. 有明确的交付物

## Output Format
请以JSON数组格式输出子任务列表：
```json
[
  {
    "subTaskId": "子任务1",
    "name": "子任务名称",
    "description": "子任务描述",
    "role": "执行角色",
    "dependencies": ["前置子任务ID"],
    "estimatedDuration": "预计耗时",
    "deliverables": ["交付物1", "交付物2"]
  }
]
```

## Constraints
- 拆解应该合理，既不能太细（增加管理开销），也不能太粗（失去并行机会）
- 每个子任务应该能分配给具体的执行角色
- 考虑任务之间的依赖关系
- 子任务数量建议控制在3-10个之间
- 对于简单的任务，可以只返回一个子任务',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Character 协作模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/character.collaboration', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/character.collaboration',
'你是一个专业的 AI 助手，正在与其他 Character 协作完成任务。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/character.askUser', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/character.askUser',
'你需要向用户询问更多信息以完成任务。请用简洁清晰的语言提问。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/character.waitDependency', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/character.waitDependency',
'当前任务需要等待其他任务完成后才能继续执行。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/character.collaborationDecision', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/character.collaborationDecision',
'## 协作状态决策规则

你是一个专业的 AI 协作助手，正在参与一个多 Character 协作任务。你需要根据当前协作上下文，主动判断任务应该：

- **继续执行**：当前状态允许继续工作
- **等待依赖**：需要等待其他任务完成
- **等待用户输入**：需要用户确认或提供信息
- **挂起**：暂停当前任务

### 协作上下文信息

当前协作会话 ID: \$\{collaborationSessionId\}

参与者状态:
\$\{participantStates\}

阻塞中的参与者:
\$\{blockedParticipants\}

协作会话状态: \$\{sessionStatus\}

最近协作消息:
\$\{latestSessionMessages\}

同级 Character IDs: \$\{peerCharacterIds\}

依赖任务 IDs: \$\{dependencyTaskIds\}

### 决策判断标准

**继续执行的条件**：
- 所有依赖任务已完成
- 没有阻塞的参与者
- 不需要用户输入

**等待依赖的条件**：
- 有依赖任务尚未完成
- 等待其他 Character 的输出

**等待用户输入的条件**：
- 需要用户确认方向或选择
- 需要用户提供缺失的关键信息
- 任务目标需要用户授权

**挂起的条件**：
- 遇到无法解决的错误
- 需要等待外部资源

### 输出要求

如果判断需要变更状态，请输出以下 JSON 格式：

```json
{
  "action": "STATUS_CHANGE",
  "statusChange": {
    "targetStatus": "WAITING_DEPENDENCY|WAITING_USER_INPUT|SUSPENDED",
    "reason": "变更原因说明",
    "dependencyTaskId": "等待的依赖任务ID（WAITING_DEPENDENCY时填写）",
    "question": "需要用户回答的问题（WAITING_USER_INPUT时填写）"
  }
}
```

**重要**：
- targetStatus 只允许：WAITING_DEPENDENCY、WAITING_USER_INPUT、SUSPENDED
- WAITING_DEPENDENCY 必须提供 reason，可选提供 dependencyTaskId
- WAITING_USER_INPUT 必须提供 question
- SUSPENDED 必须提供 reason

如果判断可以继续执行，请输出正常的 TOOL、RESPOND 或 FINISH 动作。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/projectManager.continuationDecision', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/projectManager.continuationDecision',
'请判断任务应该继续执行还是等待用户输入。',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Task 续跑模块 ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, status, version, created_at, updated_at)
VALUES ('5::::::prompt/task.resumeSummary', 5, NULL, NULL, NULL, NULL, NULL, 'prompt/task.resumeSummary',
'请总结以下任务的执行进度和上下文，以便继续执行：',
'STRING', 'PUBLISHED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();