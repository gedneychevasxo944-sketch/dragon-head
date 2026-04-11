-- ============================================================================
-- Dragon Head 冷启动数据
-- 版本: V2
-- 说明: 包含 trait、permission_policy、config_store 等初始数据
--
-- 包含:
--   - Trait 种子数据（16条）
--   - Permission Policy 种子数据
--   - ConfigStore 系统配置（Character、Skill、Memory、System 配置）
--   - ConfigStore Prompt 配置
-- ============================================================================

use adeptify;

-- ============================================================================
-- System Admin 用户账号
-- 说明: 用于审批所有发布到公共库的申请
-- ============================================================================

INSERT INTO adeptify_user (username, password_hash, nickname, status, create_time) VALUES
('system', '$2a$10$qv0CcheqIC6TrU3Nc5Dh9eukmGCo75dv3XlU4ZDAtuobgKEJsl79W', 'System Admin', 'NORMAL', NOW());

-- ============================================================================
-- Trait 种子数据
-- ============================================================================

INSERT INTO trait (name, category, description, content, enabled, used_by_count, create_time) VALUES
('结构化思维', 'personality', '倾向于用逻辑和结构化的方式处理信息和问题', '你倾向于用逻辑和结构化的方式处理信息和问题。在分析和解决问题时，你会先梳理框架，再填充细节。', true, 0, NOW()),
('数据驱动', 'personality', '决策基于数据分析而非直觉', '你是一个数据驱动的人，决策时会优先考虑数据和分析结果，而非直觉。你会用数据来验证假设和支持结论。', true, 0, NOW()),
('风险管理意识', 'personality', '主动识别和评估潜在风险', '你具有强烈的风险管理意识，会主动识别和评估潜在风险。在做决策前，你会考虑各种可能的风险因素。', true, 0, NOW()),
('批判性思维', 'personality', '不轻信信息，善于质疑和分析', '你具有批判性思维，不轻信信息，善于质疑和分析。你会对信息进行深入思考，而非盲目接受。', true, 0, NOW()),
('高效协作', 'personality', '擅长与他人合作，共同完成任务', '你擅长与他人合作，能够有效协调团队资源，共同完成复杂任务。你注重沟通和分工配合。', true, 0, NOW()),
('温暖同理', 'personality', '能够理解和感受他人情绪', '你能够理解和感受他人情绪，与人交流时富有同理心。你善于倾听，能感知对方的真实需求。', true, 0, NOW()),
('耐心引导', 'personality', '不急躁，愿意花时间解释和引导', '你耐心细致，不急躁，愿意花时间解释和引导他人。你相信循序渐进的力量。', true, 0, NOW()),
('创意发散', 'personality', '思维活跃，善于产生新颖想法', '你思维活跃，善于产生新颖的想法和创意。你不拘泥于常规，能够提供独特的视角和解决方案。', true, 0, NOW()),
('简洁表达', 'personality', '追求简洁明了的表达方式', '你追求简洁明了的表达方式，用最精炼的语言传达核心信息。你相信简洁是智慧的灵魂。', true, 0, NOW()),
('代码质量优先', 'config', '严格遵循代码规范和最佳实践', '你严格遵循代码规范和最佳实践，注重代码的可读性、可维护性和性能。你会进行代码审查并提出改进建议。', true, 0, NOW()),
('性能意识', 'config', '关注系统性能和资源效率', '你关注系统性能和资源效率，会从性能角度审视设计和实现。你善于发现和解决性能瓶颈。', true, 0, NOW()),
('学术严谨', 'config', '引用规范，内容经过验证', '你注重学术严谨性，引用规范，内容经过验证。你会确保信息的准确性和可靠性。', true, 0, NOW()),
('用户中心', 'personality', '始终以用户价值为出发点', '你始终以用户价值为出发点，在做决策时会优先考虑用户需求和使用体验。你相信为用户创造价值是核心目标。', true, 0, NOW()),
('迭代思维', 'personality', '小步快跑，持续改进', '你信奉迭代思维，倾向于小步快跑、持续改进。你相信完美的方案是通过不断迭代打磨出来的。', true, 0, NOW()),
('全渠道营销', 'config', '覆盖多个营销渠道的整合能力', '你具备全渠道营销能力，能够整合和协调多个营销渠道的策略和执行。你熟悉各渠道的特点和最佳实践。', true, 0, NOW()),
('品牌叙事', 'personality', '擅长讲故事，建立情感连接', '你擅长讲故事，能够通过叙事建立与受众的情感连接。你善于用故事来传达品牌价值和理念。', true, 0, NOW());

-- ============================================================================
-- Trait 冷启数据的发布状态
-- 说明: 为冷启 trait 创建 PUBLISHED 状态，使其在公共库可见
-- ============================================================================

INSERT INTO asset_publish_status (id, resource_type, resource_id, status, version, published_at, published_by, snapshot, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000001', 'TRAIT', '1', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000002', 'TRAIT', '2', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000003', 'TRAIT', '3', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000004', 'TRAIT', '4', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000005', 'TRAIT', '5', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000006', 'TRAIT', '6', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000007', 'TRAIT', '7', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000008', 'TRAIT', '8', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000009', 'TRAIT', '9', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000010', 'TRAIT', '10', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000011', 'TRAIT', '11', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000012', 'TRAIT', '12', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000013', 'TRAIT', '13', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000014', 'TRAIT', '14', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000015', 'TRAIT', '15', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW()),
('00000000-0000-0000-0000-000000000016', 'TRAIT', '16', 'PUBLISHED', 1, NOW(), 'system', NULL, NOW(), NOW());

-- ============================================================================
-- Permission Policy 种子数据
-- ============================================================================

INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WILDCARD', 'OWNER', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR", "TRANSFER"]');

INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WORKSPACE', 'ADMIN', '["VIEW", "USE", "EDIT", "MANAGE_COLLABORATOR"]'),
('CHARACTER', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]'),
('SKILL', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]'),
('TOOL', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]'),
('OBSERVER', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "MANAGE_COLLABORATOR"]'),
('CONFIG', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE"]'),
('MODEL', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]'),
('TEMPLATE', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]'),
('COMMONSENSE', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE"]'),
('TRAIT', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE"]');

INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WILDCARD', 'COLLABORATOR', '["VIEW", "USE"]');

INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WORKSPACE', 'MEMBER', '["VIEW"]'),
('CHARACTER', 'MEMBER', '["VIEW"]'),
('OBSERVER', 'MEMBER', '["VIEW"]'),
('CONFIG', 'MEMBER', '["VIEW"]'),
('COMMONSENSE', 'MEMBER', '["VIEW"]');

-- ============================================================================
-- ConfigStore 系统配置
-- ============================================================================

-- ID 格式: {scopeBit}:{workspaceId}:{characterId}:{toolId}:{skillId}:{memoryId}:{configKey}
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('1::::::jwt.secret', 1, NULL, NULL, NULL, NULL, NULL, 'jwt.secret', 'adeptify-256-bit-secret-key-for-jwt-signing-must-be-long-enough-2024', 'STRING', 'JWT Secret', 'JWT 签名密钥（生产环境请使用安全的随机字符串）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::jwt.access-token-validity', 1, NULL, NULL, NULL, NULL, NULL, 'jwt.access-token-validity', '7200', 'NUMBER', 'Access Token Validity', 'Access Token 有效期（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::jwt.refresh-token-validity', 1, NULL, NULL, NULL, NULL, NULL, 'jwt.refresh-token-validity', '604800', 'NUMBER', 'Refresh Token Validity', 'Refresh Token 有效期（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();


-- ==================== Character 配置 (scopeBit=1) ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::maxSteps', 1, NULL, NULL, NULL, NULL, NULL, 'maxSteps', '10', 'NUMBER', '最大迭代次数', 'Character执行ReAct循环的最大步数，超过后强制终止', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::maxIterations', 1, NULL, NULL, NULL, NULL, NULL, 'maxIterations', '10', 'NUMBER', '最大循环次数', '任务执行的最大循环迭代次数，防止无限循环', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::enableMemorySearch', 1, NULL, NULL, NULL, NULL, NULL, 'enableMemorySearch', 'true', 'BOOLEAN', '启用记忆搜索', '是否在执行任务时启用记忆检索功能', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::enableToolUse', 1, NULL, NULL, NULL, NULL, NULL, 'enableToolUse', 'true', 'BOOLEAN', '启用工具使用', '是否允许Character调用工具执行操作', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Skill 配置 (scopeBit=1) ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::maxSingleFileBytes', 1, NULL, NULL, NULL, NULL, NULL, 'maxSingleFileBytes', '2097152', 'NUMBER', '单文件最大字节数', 'Skill处理的最大单个文件大小限制，默认2MB', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::maxZipFileBytes', 1, NULL, NULL, NULL, NULL, NULL, 'maxZipFileBytes', '10485760', 'NUMBER', 'ZIP文件最大字节数', 'Skill处理的ZIP压缩包最大大小限制，默认10MB', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::maxUnzipTotalBytes', 1, NULL, NULL, NULL, NULL, NULL, 'maxUnzipTotalBytes', '52428800', 'NUMBER', '解压总字节数限制', 'Skill处理时解压文件的最大总字节数限制，默认50MB', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::maxFileCount', 1, NULL, NULL, NULL, NULL, NULL, 'maxFileCount', '100', 'NUMBER', '最大文件数量', 'Skill处理时操作的最大文件数量限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::askTimeoutMs', 1, NULL, NULL, NULL, NULL, NULL, 'askTimeoutMs', '30000', 'NUMBER', '询问超时毫秒', 'Skill向用户发送询问消息的超时时间，默认30秒', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Memory 配置 (scopeBit=1) ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::similarityThreshold', 1, NULL, NULL, NULL, NULL, NULL, 'similarityThreshold', '0.7', 'NUMBER', '相似度阈值', '记忆检索时的相似度匹配阈值，范围0-1', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== 系统级配置 (scopeBit=1) ====================

-- Schedule 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::schedule.defaultSeconds', 1, NULL, NULL, NULL, NULL, NULL, 'schedule.defaultSeconds', '0', 'NUMBER', '调度默认秒数', '任务调度执行的默认时间间隔（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Workflow 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::workflow.maxIterations', 1, NULL, NULL, NULL, NULL, NULL, 'workflow.maxIterations', '10', 'NUMBER', '工作流最大迭代次数', '工作流引擎执行的最大迭代次数限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Evaluation 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::evaluation.maxDurationMs', 1, NULL, NULL, NULL, NULL, NULL, 'evaluation.maxDurationMs', '100000', 'NUMBER', '评估最大耗时', '评估任务执行的超时时间（毫秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::evaluation.maxTokens', 1, NULL, NULL, NULL, NULL, NULL, 'evaluation.maxTokens', '100000', 'NUMBER', '评估最大Token数', '评估任务消耗的最大Token数量限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Bash 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::bash.defaultJobTtlMs', 1, NULL, NULL, NULL, NULL, NULL, 'bash.defaultJobTtlMs', '1800000', 'NUMBER', 'Bash默认任务TTL', 'Bash任务默认存活时间（毫秒），默认30分钟', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::bash.minJobTtlMs', 1, NULL, NULL, NULL, NULL, NULL, 'bash.minJobTtlMs', '60000', 'NUMBER', 'Bash最小任务TTL', 'Bash任务允许的最小存活时间（毫秒），默认1分钟', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::bash.maxJobTtlMs', 1, NULL, NULL, NULL, NULL, NULL, 'bash.maxJobTtlMs', '10800000', 'NUMBER', 'Bash最大任务TTL', 'Bash任务允许的最大存活时间（毫秒），默认3小时', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::bash.defaultMaxOutputChars', 1, NULL, NULL, NULL, NULL, NULL, 'bash.defaultMaxOutputChars', '50000', 'NUMBER', 'Bash默认最大输出字符数', 'Bash任务默认最大输出字符数限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::bash.defaultTailChars', 1, NULL, NULL, NULL, NULL, NULL, 'bash.defaultTailChars', '2000', 'NUMBER', 'Bash默认尾部字符数', 'Bash任务输出截取尾部字符数，默认展示最后2000字符', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Web 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::web.maxBodyChars', 1, NULL, NULL, NULL, NULL, NULL, 'web.maxBodyChars', '50000', 'NUMBER', 'Web最大Body字符数', 'Web请求/响应Body的最大字符数限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::web.requestTimeout', 1, NULL, NULL, NULL, NULL, NULL, 'web.requestTimeout', '30', 'NUMBER', 'Web请求超时秒数', 'Web请求超时时间（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::web.searchTimeout', 1, NULL, NULL, NULL, NULL, NULL, 'web.searchTimeout', '15', 'NUMBER', 'Web搜索超时秒数', 'Web搜索操作超时时间（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Exec 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::exec.defaultTimeoutSeconds', 1, NULL, NULL, NULL, NULL, NULL, 'exec.defaultTimeoutSeconds', '120', 'NUMBER', '执行默认超时秒数', '通用执行任务的默认超时时间（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::exec.maxOutputLength', 1, NULL, NULL, NULL, NULL, NULL, 'exec.maxOutputLength', '50000', 'NUMBER', '执行最大输出长度', '执行任务的最大输出字符数限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- FileTools 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::filetools.maxMatches', 1, NULL, NULL, NULL, NULL, NULL, 'filetools.maxMatches', '50', 'NUMBER', '文件工具最大匹配数', '文件搜索操作返回的最大匹配结果数', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Tool 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::tool.maxDetailEntries', 1, NULL, NULL, NULL, NULL, NULL, 'tool.maxDetailEntries', '8', 'NUMBER', '工具最大详情条目数', '工具返回结果的最大详情条目数限制', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Sandbox 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.workspaceRoot', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.workspaceRoot', '~/.dragon/sandboxes', 'STRING', '沙箱工作目录根路径', '沙箱容器的工作目录根路径', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.dockerImage', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.dockerImage', 'dragonhead-sandbox', 'STRING', '沙箱Docker镜像', '沙箱容器使用的Docker镜像名称', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.containerPrefix', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.containerPrefix', 'dragon-sbx-', 'STRING', '沙箱容器前缀', '沙箱容器名称的前缀标识', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.idleHours', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.idleHours', '24', 'NUMBER', '沙箱空闲小时数', '沙箱容器空闲多少小时后自动回收（小时）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.maxAgeDays', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.maxAgeDays', '7', 'NUMBER', '沙箱最大保留天数', '沙箱容器最大保留天数，超过后自动删除', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.cdpPort', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.cdpPort', '9222', 'NUMBER', '沙箱CDP端口', 'Chrome DevTools Protocol调试端口', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.vncPort', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.vncPort', '5900', 'NUMBER', '沙箱VNC端口', 'VNC远程桌面连接端口', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sandbox.novncPort', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.novncPort', '6080', 'NUMBER', '沙箱noVNC端口', 'Web VNC访问端口', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- User 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::user.maxLoginFailCount', 1, NULL, NULL, NULL, NULL, NULL, 'user.maxLoginFailCount', '5', 'NUMBER', '用户最大登录失败次数', '用户登录失败次数超过此值后锁定账户', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::user.lockMinutes', 1, NULL, NULL, NULL, NULL, NULL, 'user.lockMinutes', '15', 'NUMBER', '用户锁定分钟数', '用户账户被锁定的时间（分钟）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- SMS 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sms.codeValidityMinutes', 1, NULL, NULL, NULL, NULL, NULL, 'sms.codeValidityMinutes', '5', 'NUMBER', '短信验证码有效期', '短信验证码有效时间（分钟）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::sms.sendCooldownSeconds', 1, NULL, NULL, NULL, NULL, NULL, 'sms.sendCooldownSeconds', '60', 'NUMBER', '短信发送冷却秒数', '同一手机号再次发送短信的冷却时间（秒）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Workspace 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::workspace.workingStyle', 1, NULL, NULL, NULL, NULL, NULL, 'workspace.workingStyle', 'COLLABORATIVE', 'STRING', '工作空间工作风格', '工作空间的协作风格（COLLABORATIVE/CONSERVATIVE/AGGRESSIVE/INNOVATIVE/ANALYTICAL）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::workspace.decisionPattern', 1, NULL, NULL, NULL, NULL, NULL, 'workspace.decisionPattern', 'CONSULTATIVE', 'STRING', '工作空间决策模式', '工作空间的决策模式（CONSULTATIVE/DEMOCRATIC/AUTOCRATIC/CONSENSUS）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::workspace.riskTolerance', 1, NULL, NULL, NULL, NULL, NULL, 'workspace.riskTolerance', '0.5', 'NUMBER', '工作空间风险容忍度', '工作空间的风险偏好设置，范围0-1', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Registry 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::registry.defaultCharacterId', 1, NULL, NULL, NULL, NULL, NULL, 'registry.defaultCharacterId', '', 'STRING', '默认Character ID', '系统默认使用的Character ID', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::registry.defaultModelId', 1, NULL, NULL, NULL, NULL, NULL, 'registry.defaultModelId', '', 'STRING', '默认模型ID', '系统默认使用的AI模型ID', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::registry.defaultObserverId', 1, NULL, NULL, NULL, NULL, NULL, 'registry.defaultObserverId', '', 'STRING', '默认Observer ID', '系统默认使用的Observer ID', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- Observer 配置
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::observer.optimizationThreshold', 1, NULL, NULL, NULL, NULL, NULL, 'observer.optimizationThreshold', '0.6', 'NUMBER', '优化阈值', '触发优化建议的最低评分阈值，范围0-1', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::observer.consecutiveLowScoreThreshold', 1, NULL, NULL, NULL, NULL, NULL, 'observer.consecutiveLowScoreThreshold', '3', 'NUMBER', '连续低分阈值', '连续多少次低分评分后触发优化建议', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::observer.periodicEvaluationHours', 1, NULL, NULL, NULL, NULL, NULL, 'observer.periodicEvaluationHours', '24', 'NUMBER', '定期评估间隔', '定期评估任务执行的时间间隔（小时）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::observer.manualApprovalRequired', 1, NULL, NULL, NULL, NULL, NULL, 'observer.manualApprovalRequired', 'true', 'BOOLEAN', '需要人工审批', '是否需要人工审批Observer的优化建议', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::observer.planWindowHours', 1, NULL, NULL, NULL, NULL, NULL, 'observer.planWindowHours', '24', 'NUMBER', '计划窗口小时数', 'Observer观察计划的时间窗口（小时）', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::observer.maxPlanItems', 1, NULL, NULL, NULL, NULL, NULL, 'observer.maxPlanItems', '50', 'NUMBER', '最大计划条目数', 'Observer生成的最大计划条目数量', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ============================================================================
-- ConfigStore Prompt 配置
-- ============================================================================

-- ==================== Observer 模块 Prompts ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/observer.suggestion', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/observer.suggestion',
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
'STRING', 'Observer优化建议Prompt', '用于生成Character或Organization优化的具体建议，基于任务执行数据和评估记录', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/observer.personalityEnhancement', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/observer.personalityEnhancement',
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
'STRING', 'Observer Personality增强Prompt', '用于根据优化建议更新Character或Organization的性格描述字段', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== ReAct 模块 Prompts ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/react.taskDecompose', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/react.taskDecompose',
'你是一个组织调度专家，负责把复杂任务拆解为可执行的子任务。',
'STRING', 'ReAct任务拆解Prompt', '将复杂任务拆解为可执行子任务的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/react.execute', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/react.execute',
'你是 ReAct 执行阶段的 PromptWriter，需要基于给定的模板、任务上下文、历史思考、动作、观察结果和工具信息，生成当前这一轮真正给执行模型使用的提示词。
生成要求：
1. 提示词必须明确用户目标、当前进度以及最新观察结果。
2. 要先引导执行模型判断"最新观察结果是否可用，是否已经足以完成任务"。
3. 如果最新观察结果已经足够完成任务，提示模型优先输出 RESPOND 或 FINISH，并在 response 字段里给出结果。
4. 如果最新观察结果不可用、为空或表现为错误，提示模型不要结束，而是重新规划 TOOL 或 MEMORY 动作。
5. 提示词中必须包含 JSON 输出约束，字段至少包含 action、tool、params、response。
6. 你只输出最终拼装好的提示词，不要解释你的拼装过程，不要输出 Markdown 代码块。',
'STRING', 'ReAct执行Prompt', 'ReAct执行阶段生成给执行模型使用的提示词', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Character 模块 Prompts ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/character.system', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/character.system',
'你是一个专业的AI数字员工，有自己的性格特点和价值观。',
'STRING', 'Character系统Prompt', 'Character的全局系统提示词，定义AI数字员工的基础人设', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/character.task', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/character.task',
'请根据要求完成以下任务：',
'STRING', 'Character任务Prompt', 'Character执行任务时的用户消息前缀Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== HR 模块 Prompts ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/hr.hire.decision', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.hire.decision',
'请评估是否应该雇佣以下 Character 到工作空间：

Character 名称: %s
Character 描述: %s

请返回以下格式的决策：
- APPROVE：批准雇佣
- DENY：拒绝雇佣
- 需要更多信息

如果批准，请简要说明理由。',
'STRING', 'HR雇佣决策Prompt', '评估是否应该雇佣特定Character到工作空间的决策Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/hr.hire.select', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.hire.select',
'请从以下候选 Character 中选择一个最合适雇佣的：',
'STRING', 'HR雇佣选择Prompt', '从多个候选Character中选择最合适雇佣的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/hr.fire.decision', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.fire.decision',
'请评估是否应该解雇工作空间中的以下 Character：

Character ID: %s

请返回以下格式的决策：
- APPROVE：批准解雇
- DENY：拒绝解雇
- 需要更多信息

如果批准，请简要说明理由。',
'STRING', 'HR解雇决策Prompt', '评估是否应该解雇工作空间中特定Character的决策Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/hr.duty.generate', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/hr.duty.generate',
'请为以下 Character 生成一个合适的职责描述：

Character 名称: %s
Character 描述: %s

请用 1-2 句话简洁描述该 Character 在工作空间中的职责。',
'STRING', 'HR职责生成Prompt', '为Character生成在工作空间中职责描述的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== 选择模块 Prompt ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/selection.generic', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/selection.generic',
'请从以下候选中选择一个最合适的：',
'STRING', '通用选择Prompt', '通用的从候选列表中选择最合适项的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== MemberSelector 模块 Prompt ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/memberSelector.select', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/memberSelector.select',
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
'STRING', 'MemberSelector选择Prompt', '从Workspace已雇佣的Character中选择最合适执行者的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== ProjectManager 模块 Prompt ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/projectManager.decompose', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/projectManager.decompose',
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
'STRING', 'ProjectManager任务拆解Prompt', '将复杂任务拆解为可执行子任务列表的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Character 协作模块 Prompts ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/character.collaboration', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/character.collaboration',
'你是一个专业的 AI 助手，正在与其他 Character 协作完成任务。',
'STRING', 'Character协作Prompt', 'Character参与多Character协作任务时的系统Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/character.askUser', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/character.askUser',
'你需要向用户询问更多信息以完成任务。请用简洁清晰的语言提问。',
'STRING', 'Character询问用户Prompt', 'Character需要向用户询问更多信息时使用的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/character.waitDependency', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/character.waitDependency',
'当前任务需要等待其他任务完成后才能继续执行。',
'STRING', 'Character等待依赖Prompt', 'Character任务需等待其他任务完成时使用的提示Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/character.collaborationDecision', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/character.collaborationDecision',
'## 协作状态决策规则

你是一个专业的 AI 协作助手，正在参与一个多 Character 协作任务。你需要根据当前协作上下文，主动判断任务应该：

- **继续执行**：当前状态允许继续工作
- **等待依赖**：需要等待其他任务完成
- **等待用户输入**：需要用户确认或提供信息
- **挂起**：暂停当前任务

### 协作上下文信息

当前协作会话 ID: \${collaborationSessionId}

参与者状态:
\${participantStates}

阻塞中的参与者:
\${blockedParticipants}

协作会话状态: \${sessionStatus}

最近协作消息:
\${latestSessionMessages}

同级 Character IDs: \${peerCharacterIds}

依赖任务 IDs: \${dependencyTaskIds}

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
'STRING', 'Character协作状态决策Prompt', '多Character协作时判断任务状态（继续/等待/挂起）的决策Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/projectManager.continuationDecision', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/projectManager.continuationDecision',
'请判断任务应该继续执行还是等待用户输入。',
'STRING', 'ProjectManager续跑决策Prompt', '判断任务应继续执行还是等待用户输入的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Task 续跑模块 Prompt ====================

INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES ('1::::::prompt/task.resumeSummary', 1, NULL, NULL, NULL, NULL, NULL, 'prompt/task.resumeSummary',
'请总结以下任务的执行进度和上下文，以便继续执行：',
'STRING', 'Task续跑摘要Prompt', '任务中断后恢复执行时生成执行进度摘要的Prompt', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();
