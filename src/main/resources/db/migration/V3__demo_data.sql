-- ============================================================================
-- Dragon Head 前后端联调 Mock 数据
-- 版本: V3
-- 说明: 用于前端联调的完整 mock 数据
--
-- 包含:
--   - 测试用户 (adeptify_user)
--   - 工作空间 (workspace)
--   - 角色 (character)
--   - 技能 (skills)
--   - 工具 (tool)
--   - 观察者 (observer)
--   - 记忆 (common_sense)
--   - 任务 (task)
--   - 通知 (notification)
--   - 资产成员关系 (asset_member)
--   - Workspace 成员 (workspace_member)
--   - Trait 种子数据
-- ============================================================================

use adeptify;

-- ============================================================================
-- 0. 清理旧数据（重新插入时使用）
-- ============================================================================
DELETE FROM notification WHERE user_id = 1;
DELETE FROM task WHERE workspace_id IN ('ws-demo', 'ws-test');
DELETE FROM common_sense WHERE workspace_id IN ('ws-demo', 'ws-test');
DELETE FROM workspace_member WHERE workspace_id IN ('ws-demo', 'ws-test');
DELETE FROM asset_member WHERE user_id = 1;
DELETE FROM workspace WHERE id IN ('ws-demo', 'ws-test');
DELETE FROM observer WHERE id IN ('obs-001', 'obs-test');
DELETE FROM tool WHERE name IN ('tool-001', 'tool-code-review', 'tool-data-export');
DELETE FROM skills WHERE skill_id IN ('skill-001', 'skill-002', 'skill-data-analysis');
DELETE FROM `character` WHERE id IN ('char-001', 'char-002', 'char-003', 'char-pm', 'char-analyst');
DELETE FROM adeptify_user WHERE id = 1;
DELETE FROM trait WHERE name IN ('结构化思维', '数据驱动', '风险管理意识');

-- ============================================================================
-- 1. 测试用户
-- ============================================================================
-- user:testuser, password:password
INSERT INTO adeptify_user (id, username, phone, password_hash, nickname, avatar, status, login_fail_count, create_time, update_time)
VALUES
  (1, 'testuser', '13800138000', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '测试用户', 'https://api.dicebear.com/7.x/avataaars/svg?seed=testuser', 'NORMAL', 0, NOW(), NOW());

-- ============================================================================
-- 2. 工作空间 (Workspace)
-- ============================================================================
INSERT INTO workspace (id, name, description, owner, status, visibility, properties, personality, created_at, updated_at)
VALUES
  ('ws-demo', '演示工作空间', '用于前后端联调的演示工作空间，包含多个角色和技能配置', '1', 'ACTIVE', 'PRIVATE',
   '{"visibility": "PRIVATE", "maxMembers": 10, "features": ["task", "observer", "memory"]}',
   '{"workingStyle": "COLLABORATIVE", "decisionPattern": "CONSULTATIVE", "riskTolerance": 0.5}',
   NOW(), NOW()),

  ('ws-test', '测试工作空间', '用于自动化测试的工作空间', '1', 'ACTIVE', 'PRIVATE',
   '{"visibility": "PRIVATE", "maxMembers": 5}',
   '{"workingStyle": "ANALYTICAL", "decisionPattern": "CONSENSUS", "riskTolerance": 0.3}',
   NOW(), NOW());

-- ============================================================================
-- 3. 角色 (Character)
-- ============================================================================
INSERT INTO `character` (id, workspace_ids, organization_ids, name, version, description, avatar, source, allowed_tools, traits, trait_configs, skills, prompt_template, default_tools, is_running, deployed_count, mind_config, extensions, status, created_at, updated_at)
VALUES
  ('char-001', '["ws-demo"]', NULL, 'AI 项目经理', 1,
   '专业的 AI 项目管理者，能够协调多个 AI Agent 之间的工作，擅长资源分配和进度跟踪。',
   'https://api.dicebear.com/7.x/personas/svg?seed=char-pm',
   'built_in_derived',
   '["task_tracker", "calendar", "report_generator"]',
   '["结构化思维", "高效协作", "风险管理意识"]',
   '{"结构化思维": {"intensity": 0.8}, "高效协作": {"approach": "structured"}}',
   '["skill-001", "skill-002"]',
   '你是一位专业的 AI 项目管理者。你的职责是协调团队工作、跟踪进度、识别风险。',
   '["task_tracker", "calendar", "report_generator"]',
   TRUE, 2, NULL, NULL, 'RUNNING', NOW(), NOW()),

  ('char-002', '["ws-demo"]', NULL, '数据分析师', 1,
   '数据分析专家，专注于从复杂数据中提取洞察，支持决策制定。',
   'https://api.dicebear.com/7.x/personas/svg?seed=char-analyst',
   'built_in_derived',
   '["data_connector", "chart_generator", "sql_query"]',
   '["数据驱动", "学术严谨", "简洁表达"]',
   '{"数据驱动": {"methodology": "statistical"}}',
   '["skill-001", "skill-data-analysis"]',
   '你是一位资深数据分析师，擅长使用统计学方法和可视化技术分析数据。',
   '["data_connector", "chart_generator", "sql_query"]',
   TRUE, 1, NULL, NULL, 'RUNNING', NOW(), NOW()),

  ('char-003', '["ws-demo", "ws-test"]', NULL, '客服代表', 1,
   '温柔的客服代表，善于倾听和理解客户需求，提供贴心的解决方案。',
   'https://api.dicebear.com/7.x/personas/svg?seed=char-support',
   'trait_composed',
   '["ticket_system", "knowledge_base"]',
   '["温暖同理", "耐心引导", "简洁表达"]',
   '{"温暖同理": {"tone": "warm"}}',
   '["skill-001"]',
   '你是一位热情的客服代表，始终以客户满意为首要目标。',
   '["ticket_system", "knowledge_base", "response_generator"]',
   FALSE, 3, NULL, NULL, 'RUNNING', NOW(), NOW()),

  ('char-pm', '["ws-demo"]', NULL, '产品经理', 1,
   '经验丰富的产品经理，擅长需求分析和产品规划。',
   'https://api.dicebear.com/7.x/personas/svg?seed=char-product',
   'whiteboard_new',
   '["roadmap_planner", "user_research"]',
   '["创意发散", "用户中心", "迭代思维"]',
   '{}',
   '[]',
   '你是一位经验丰富的的产品经理，擅长用户需求分析和产品路线图规划。',
   '["roadmap_planner", "user_research"]',
   FALSE, 0, NULL, NULL, 'RUNNING', NOW(), NOW()),

  ('char-analyst', '["ws-test"]', NULL, '市场分析师', 1,
   '专注于市场趋势分析和竞争情报收集。',
   'https://api.dicebear.com/7.x/personas/svg?seed=char-market',
   'built_in_derived',
   '["market_tracker", "competitor_analysis"]',
   '["数据驱动", "批判性思维", "全渠道营销"]',
   '{}',
   '[]',
   '你是一位专业的市场分析师，擅长追踪市场趋势和竞争情报。',
   '["market_tracker", "competitor_analysis"]',
   FALSE, 0, NULL, NULL, 'RUNNING', NOW(), NOW());

-- ============================================================================
-- 4. Trait 种子数据
-- ============================================================================
INSERT INTO trait (name, category, description, content, enabled, used_by_count, create_time, update_time)
VALUES
  ('结构化思维', 'personality', '倾向于用逻辑和结构化的方式处理信息和问题', '你倾向于用逻辑和结构化的方式处理信息和问题。在分析和解决问题时，你会先梳理框架，再填充细节。', TRUE, 2, NOW(), NOW()),
  ('数据驱动', 'personality', '决策基于数据分析而非直觉', '你是一个数据驱动的人，决策时会优先考虑数据和分析结果，而非直觉。你会用数据来验证假设和支持结论。', TRUE, 2, NOW(), NOW()),
  ('风险管理意识', 'personality', '主动识别和评估潜在风险', '你具有强烈的风险管理意识，会主动识别和评估潜在风险。在做决策前，你会考虑各种可能的风险因素。', TRUE, 1, NOW(), NOW()),
  ('批判性思维', 'personality', '不轻信信息，善于质疑和分析', '你具有批判性思维，不轻信信息，善于质疑和分析。你会对信息进行深入思考，而非盲目接受。', TRUE, 1, NOW(), NOW()),
  ('高效协作', 'personality', '擅长与他人合作，共同完成任务', '你擅长与他人合作，能够有效协调团队资源，共同完成复杂任务。你注重沟通和分工配合。', TRUE, 1, NOW(), NOW()),
  ('温暖同理', 'personality', '能够理解和感受他人情绪', '你能够理解和感受他人情绪，与人交流时富有同理心。你善于倾听，能感知对方的真实需求。', TRUE, 1, NOW(), NOW()),
  ('耐心引导', 'personality', '不急躁，愿意花时间解释和引导', '你耐心细致，不急躁，愿意花时间解释和引导他人。你相信循序渐进的力量。', TRUE, 1, NOW(), NOW()),
  ('创意发散', 'personality', '思维活跃，善于产生新颖想法', '你思维活跃，善于产生新颖的想法和创意。你不拘泥于常规，能够提供独特的视角和解决方案。', TRUE, 1, NOW(), NOW()),
  ('简洁表达', 'personality', '追求简洁明了的表达方式', '你追求简洁明了的表达方式，用最精炼的语言传达核心信息。你相信简洁是智慧的灵魂。', TRUE, 2, NOW(), NOW()),
  ('用户中心', 'personality', '始终以用户价值为出发点', '你始终以用户价值为出发点，在做决策时会优先考虑用户需求和使用体验。你相信为用户创造价值是核心目标。', TRUE, 1, NOW(), NOW()),
  ('迭代思维', 'personality', '小步快跑，持续改进', '你信奉迭代思维，倾向于小步快跑、持续改进。你相信完美的方案是通过不断迭代打磨出来的。', TRUE, 1, NOW(), NOW()),
  ('学术严谨', 'config', '引用规范，内容经过验证', '你注重学术严谨性，引用规范，内容经过验证。你会确保信息的准确性和可靠性。', TRUE, 1, NOW(), NOW()),
  ('全渠道营销', 'config', '覆盖多个营销渠道的整合能力', '你具备全渠道营销能力，能够整合和协调多个营销渠道的策略和执行。你熟悉各渠道的特点和最佳实践。', TRUE, 1, NOW(), NOW());

-- ============================================================================
-- 5. 技能 (Skills)
-- ============================================================================
INSERT INTO skills (skill_id, name, display_name, description, content, category, visibility, creator_type, creator_id, creator_name, status, version, tags, created_at, published_at)
VALUES
  ('skill-001', '通用助手', '通用助手', '提供通用的 AI 助手能力，支持多种任务类型',
   '# 通用助手技能\n\n## 能力\n- 问答对话\n- 任务分解\n- 信息检索\n- 文本生成',
   '通用', 'public', 'official', NULL, '官方助手', 'RUNNING', 1,
   '["助手", "通用", "问答"]', NOW(), NOW()),

  ('skill-002', '任务管理', '任务管理技能', '专业的任务管理和项目跟踪能力',
   '# 任务管理技能\n\n## 能力\n- 创建和分解任务\n- 跟踪任务进度\n- 识别任务依赖\n- 风险预警',
   '项目管理', 'public', 'official', NULL, '官方助手', 'RUNNING', 2,
   '["任务", "管理", "项目"]', NOW(), NOW()),

  ('skill-data-analysis', '数据分析', '数据分析技能', '从复杂数据中提取洞察，支持决策',
   '# 数据分析技能\n\n## 能力\n- 数据清洗和预处理\n- 统计分析和建模\n- 可视化生成\n- 洞察提炼',
   '数据分析', 'public', 'official', NULL, '官方助手', 'RUNNING', 1,
   '["数据", "分析", "可视化"]', NOW(), NOW());

-- ============================================================================
-- 6. 工具 (Tool)
-- ============================================================================
INSERT INTO tool (name, description, parameter_schema, enabled)
VALUES
  ('task_tracker', '任务跟踪工具，用于创建、更新和查询任务', '{"type": "object", "properties": {"action": {"type": "string"}, "task_id": {"type": "string"}}}', TRUE),
  ('calendar', '日历工具，用于查看和安排日程', '{"type": "object", "properties": {"action": {"type": "string"}, "date": {"type": "string"}}}', TRUE),
  ('report_generator', '报告生成工具，自动生成各类报告', '{"type": "object", "properties": {"format": {"type": "string"}, "content": {"type": "string"}}}', TRUE),
  ('data_connector', '数据连接器，用于连接各种数据源', '{"type": "object", "properties": {"source": {"type": "string"}, "query": {"type": "string"}}}', TRUE),
  ('chart_generator', '图表生成工具，从数据生成可视化图表', '{"type": "object", "properties": {"chart_type": {"type": "string"}, "data": {"type": "array"}}}', TRUE),
  ('sql_query', 'SQL 查询工具，执行数据库查询', '{"type": "object", "properties": {"sql": {"type": "string"}}}', TRUE),
  ('ticket_system', '工单系统，用于管理和响应用户请求', '{"type": "object", "properties": {"action": {"type": "string"}, "ticket_id": {"type": "string"}}}', TRUE),
  ('knowledge_base', '知识库工具，检索和管理知识', '{"type": "object", "properties": {"query": {"type": "string"}}}', TRUE),
  ('tool-001', '代码审查工具', '{"type": "object", "properties": {"repo": {"type": "string"}, "pr_id": {"type": "string"}}}', TRUE),
  ('tool-code-review', '代码质量分析工具', '{"type": "object", "properties": {"language": {"type": "string"}, "code": {"type": "string"}}}', TRUE),
  ('tool-data-export', '数据导出工具', '{"type": "object", "properties": {"format": {"type": "string"}, "source": {"type": "string"}}}', TRUE);

-- ============================================================================
-- 7. 观察者 (Observer)
-- ============================================================================
INSERT INTO observer (id, name, description, workspace_id, status, evaluation_mode, optimization_threshold, consecutive_low_score_threshold, common_sense_enabled, auto_optimization_enabled, periodic_evaluation_hours, properties, planner_character_ids, reviewer_character_ids, supported_target_types, manual_approval_required, schedule_cron, plan_window_hours, max_plan_items, created_at, updated_at)
VALUES
  ('obs-001', '工作空间观察者', '监控工作空间整体运行状态，观察角色表现', 'ws-demo', 'RUNNING', 'AUTO', 0.6, 3, TRUE, TRUE, 24,
   '{"evaluationInterval": "daily", "alertThreshold": 0.5}',
   '["char-001"]', '["char-002"]',
   '["WORKSPACE", "CHARACTER"]', TRUE, '0 0 * * *', 24, 50, NOW(), NOW()),

  ('obs-test', '测试观察者', '用于测试的观察者', 'ws-test', 'INACTIVE', 'MANUAL', 0.7, 5, TRUE, FALSE, 48,
   '{}', NULL, NULL, '["CHARACTER"]', TRUE, NULL, 24, 30, NOW(), NOW());

-- ============================================================================
-- 8. 常识/记忆 (Common Sense)
-- ============================================================================
INSERT INTO common_sense (id, workspace_id, folder_id, name, description, category, rule, severity, version, enabled, prompt_template, prompt_variables, content, cached_prompt, last_prompt_update_at, prompt_update_source, created_at, updated_at, created_by)
VALUES
  ('cs-001', 'ws-demo', NULL, '代码审查规范', '团队代码审查的标准和规范', 'quality', '所有 PR 必须经过至少一人审查才能合并', 'HIGH', 1, TRUE,
   '请遵循以下代码审查规范：\n{{rule}}', '{"rule": "规则内容"}', '代码审查规范：\n1. 检查代码风格\n2. 验证逻辑正确性\n3. 评估性能影响\n4. 检查安全漏洞', NULL, NOW(), 'MANUAL', NOW(), NOW(), '1'),

  ('cs-002', 'ws-demo', NULL, '响应时间标准', '客户请求的响应时间要求', 'sla', '普通请求 24 小时内响应，紧急请求 4 小时内响应', 'MEDIUM', 1, TRUE,
   '响应时间标准：\n{{rule}}', NULL, '响应时间标准：\n- 普通请求：24 小时\n- 紧急请求：4 小时\n- 关键故障：1 小时', NULL, NOW(), 'MANUAL', NOW(), NOW(), '1'),

  ('cs-003', 'ws-demo', NULL, '数据安全准则', '处理敏感数据的安全和合规要求', 'security', '禁止将敏感数据写入日志，禁止在外部分享客户数据', 'HIGH', 1, TRUE,
   '数据安全准则：\n{{rule}}', NULL, '数据安全准则：\n1. 敏感数据脱敏处理\n2. 最小权限原则\n3. 数据加密存储\n4. 审计日志记录', NULL, NOW(), 'MANUAL', NOW(), NOW(), '1');

-- ============================================================================
-- 9. 任务 (Task)
-- ============================================================================
INSERT INTO task (id, workspace_id, parent_task_id, creator_id, character_id, name, description, status, input, output, result, error_message, child_task_ids, collaboration_session_id, assigned_member_ids, execution_steps, execution_messages, current_streaming_content, created_at, updated_at, started_at, completed_at, execution_mode, workflow_id, dependency_task_ids, waiting_reason, resume_token, resume_context, source_message_id, source_chat_id, source_channel, material_ids, last_question, interaction_context, metadata, extensions)
VALUES
  ('task-001', 'ws-demo', NULL, '1', 'char-001', '完成产品需求文档', '编写新功能的完整需求文档，包括用户故事和验收标准', 'COMPLETED',
   '{"feature": "新功能开发", "priority": "high"}',
   '{"document": "PRD-v1.0.pdf", "pages": 15}',
   '需求文档已完成，包含 8 个用户故事和完整的验收标准', NULL,
   '["task-002", "task-003"]', 'sess-001', '["char-001", "char-pm"]',
   '[{"stepNumber": 1, "content": "调研"}, {"stepNumber": 2, "content": "撰写"}, {"stepNumber": 3, "content": "评审"}]',
   '[{"role": "user", "content": "请完成需求文档"}, {"role": "assistant", "content": "开始撰写需求文档..."}]',
   NULL, NOW(), NOW(), NOW(), NOW(), 'AUTO', 'wf-001', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

  ('task-002', 'ws-demo', 'task-001', '1', 'char-002', '分析竞品功能', '调研竞争对手的类似功能，分析优劣势', 'COMPLETED',
   '{"competitor": "竞品A", "feature": "用户反馈系统"}',
   '{"analysis": "竞品分析报告.pdf", "pages": 10}',
   '完成竞品分析，发现 5 个可借鉴点和 3 个差异化机会', NULL,
   NULL, 'sess-001', '["char-002"]',
   '[{"stepNumber": 1, "content": "收集信息"}, {"stepNumber": 2, "content": "分析"}, {"stepNumber": 3, "content": "撰写报告"}]',
   NULL, NULL, NOW(), NOW(), NOW(), NOW(), 'AUTO', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

  ('task-003', 'ws-demo', 'task-001', '1', 'char-003', '用户访谈', '对目标用户进行访谈，收集需求反馈', 'RUNNING',
   '{"interview_type": "半结构化访谈", "participants": 5}',
   NULL, NULL, NULL,
   NULL, NULL, '["char-003"]',
   '[{"stepNumber": 1, "content": "准备访谈提纲"}, {"stepNumber": 2, "content": "执行访谈"}]',
   NULL, '正在执行第 3 场用户访谈...', NOW(), NOW(), NOW(), NULL, 'MANUAL', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

  ('task-004', 'ws-demo', NULL, '1', NULL, '周报汇总', '汇总本周工作进展，生成周报', 'PENDING',
   '{"week": "2024-W15"}',
   NULL, NULL, NULL,
   NULL, NULL, NULL,
   NULL, NULL, NULL, NOW(), NOW(), NULL, NULL, 'AUTO', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

  ('task-005', 'ws-demo', NULL, '1', 'char-002', '数据分析报告', '基于上月数据生成分析报告', 'FAILED',
   '{"data_range": "2024-03", "report_type": "monthly"}',
   NULL, NULL, '数据源连接超时，请检查网络配置',
   NULL, NULL, '["char-002"]',
   '[{"stepNumber": 1, "content": "连接数据源"}]',
   NULL, NULL, NOW(), NOW(), NOW(), NULL, 'AUTO', NULL, NULL, NULL, 'resume-token-005', '{"lastStep": 1, "retryCount": 3}', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- ============================================================================
-- 10. Workspace 成员
-- ============================================================================
INSERT INTO workspace_member (id, workspace_id, character_id, role, layer, permission, tags, weight, priority, reputation, resource_quota, join_at, last_active_at, metadata)
VALUES
  ('wm-001', 'ws-demo', 'char-001', 'PROJECT_MANAGER', 'NORMAL', 'USE', '["核心成员"]', 1.0, 1, 95,
   '{"maxTasks": 10, "maxMemory": "1GB"}', NOW(), NOW(), NULL),

  ('wm-002', 'ws-demo', 'char-002', 'DATA_ANALYST', 'NORMAL', 'USE', '["数据"]', 0.8, 2, 88,
   '{"maxTasks": 5}', NOW(), NOW(), NULL),

  ('wm-003', 'ws-demo', 'char-003', 'SUPPORT', 'NORMAL', 'USE', '["客服"]', 0.7, 3, 72,
   '{"maxTasks": 8}', NOW(), NOW(), NULL),

  ('wm-004', 'ws-demo', 'char-pm', 'PRODUCT', 'NORMAL', 'VIEW', '["产品"]', 0.6, 4, 65,
   '{}', NOW(), NOW(), NULL);

-- ============================================================================
-- 11. 资产成员关系 (Asset Member)
-- ============================================================================
INSERT INTO asset_member (resource_type, resource_id, user_id, role, invited_by, invited_at, accepted_at, accepted, created_at, updated_at)
VALUES
  -- Workspace 资产
  ('WORKSPACE', 'ws-demo', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('WORKSPACE', 'ws-test', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),

  -- Character 资产
  ('CHARACTER', 'char-001', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('CHARACTER', 'char-002', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('CHARACTER', 'char-003', 1, 'COLLABORATOR', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('CHARACTER', 'char-pm', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),

  -- Skill 资产
  ('SKILL', 'skill-001', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('SKILL', 'skill-002', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('SKILL', 'skill-data-analysis', 1, 'COLLABORATOR', '1', NOW(), NOW(), TRUE, NOW(), NOW()),

  -- Tool 资产
  ('TOOL', 'tool-001', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('TOOL', 'tool-code-review', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('TOOL', 'tool-data-export', 1, 'COLLABORATOR', '1', NOW(), NOW(), TRUE, NOW(), NOW()),

  -- Observer 资产
  ('OBSERVER', 'obs-001', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('OBSERVER', 'obs-test', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),

  -- Memory 资产
  ('MEMORY', 'cs-001', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('MEMORY', 'cs-002', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW()),
  ('MEMORY', 'cs-003', 1, 'OWNER', '1', NOW(), NOW(), TRUE, NOW(), NOW());

-- ============================================================================
-- 12. 通知 (Notification)
-- ============================================================================
INSERT INTO notification (id, user_id, type, title, content, link, source_type, source_id, is_read, created_at)
VALUES
  ('notif-001', 1, 'APPROVAL_RESULT', '任务审批已通过', '您的"完成产品需求文档"任务已通过审批', '/workspace/ws-demo/task/task-001', 'TASK', 'task-001', FALSE, NOW()),
  ('notif-002', 1, 'COLLABORATOR_INVITE', '新协作者加入', 'char-003 已接受邀请加入工作空间', '/workspace/ws-demo/character/char-003', 'WORKSPACE', 'ws-demo', TRUE, NOW()),
  ('notif-003', 1, 'SYSTEM', '系统更新通知', '系统将于本周日凌晨进行例行维护，预计耗时 2 小时', '/notice/maintenance', 'SYSTEM', NULL, FALSE, NOW()),
  ('notif-004', 1, 'APPROVAL_REQUEST', '待处理审批', '您有一条来自 char-002 的任务审批请求需要处理', '/workspace/ws-demo/approval/approval-001', 'TASK', 'task-approval-001', FALSE, NOW()),
  ('notif-005', 1, 'SYSTEM', '观察者报告', '工作空间观察者已完成本周评估，整体表现良好', '/workspace/ws-demo/observer/obs-001/report', 'OBSERVER', 'obs-001', TRUE, NOW());

-- ============================================================================
-- 13. Skill 绑定关系
-- ============================================================================
INSERT INTO skill_bindings (binding_type, character_id, workspace_id, skill_id, version_type, fixed_version, created_at)
VALUES
  ('character', 'char-001', NULL, 'skill-001', 'latest', NULL, NOW()),
  ('character', 'char-001', NULL, 'skill-002', 'latest', NULL, NOW()),
  ('character', 'char-002', NULL, 'skill-001', 'latest', NULL, NOW()),
  ('character', 'char-002', NULL, 'skill-data-analysis', 'latest', NULL, NOW()),
  ('character', 'char-003', NULL, 'skill-001', 'latest', NULL, NOW()),
  ('workspace', NULL, 'ws-demo', 'skill-001', 'latest', NULL, NOW()),
  ('workspace', NULL, 'ws-demo', 'skill-002', 'latest', NULL, NOW()),
  ('character_workspace', 'char-001', 'ws-demo', 'skill-001', 'fixed', 1, NOW());

-- ============================================================================
-- 14. Model Instance (用于 LLM 调用)
-- ============================================================================
INSERT INTO model_instance (id, provider, model_name, endpoint, credentials, default_params, enabled, description, priority)
VALUES
  ('model-kimi', 'kimi', 'moonshot-v1-8k', 'https://api.moonshot.cn/v1', '{"apiKey": "${KIMI_API_KEY:}"}', '{"temperature": 0.7, "max_tokens": 8000}', TRUE, 'Kimi 8K 上下文模型', 10),
  ('model-deepseek', 'deepseek', 'deepseek-chat', 'https://api.deepseek.com/v1', '{"apiKey": "${DEEPSEEK_API_KEY:}"}', '{"temperature": 0.7, "max_tokens": 4000}', TRUE, 'DeepSeek Chat 模型', 5);

-- ============================================================================
-- 15. ConfigStore 配置数据（各粒度）
-- ============================================================================
--
-- ID 格式: {scopeBit}:{workspaceId}:{characterId}:{toolId}:{skillId}:{memoryId}:{configKey}
--
-- scopeBit 含义:
--   GLOBAL=1, GLOBAL_WORKSPACE=5, GLOBAL_CHARACTER=9, GLOBAL_SKILL=17, GLOBAL_TOOL=33, GLOBAL_MEMORY=65
--   GLOBAL_WS_CHAR=13, GLOBAL_WS_SKILL=21, GLOBAL_WS_TOOL=37, GLOBAL_WS_MEMORY=69
--   GLOBAL_CHAR_TOOL=41, GLOBAL_CHAR_SKILL=25, GLOBAL_CHAR_MEMORY=73
--   GLOBAL_WS_CHAR_TOOL=45, GLOBAL_WS_CHAR_SKILL=29, GLOBAL_WS_CHAR_MEMORY=77
--   STUDIO_WORKSPACE=7, STUDIO_CHARACTER=11, STUDIO_SKILL=19, STUDIO_TOOL=35, STUDIO_MEMORY=67
--   STUDIO_WS_CHAR=15, STUDIO_WS_SKILL=23, STUDIO_WS_TOOL=39, STUDIO_WS_MEMORY=71
--   STUDIO_CHAR_TOOL=43, STUDIO_CHAR_SKILL=27, STUDIO_CHAR_MEMORY=75
--   OBSERVER_GLOBAL_WORKSPACE=133, OBSERVER_GLOBAL_CHARACTER=137
-- ============================================================================

-- ==================== GLOBAL (scopeBit=1) 全局配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  -- LLM 默认配置
  ('1::::::llm.defaultModel', 1, NULL, NULL, NULL, NULL, NULL, 'llm.defaultModel', 'model-kimi', 'STRING', '默认 LLM 模型', '系统默认使用的 LLM 模型', NULL, '["model-kimi", "model-deepseek"]', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::llm.temperature', 1, NULL, NULL, NULL, NULL, NULL, 'llm.temperature', '0.7', 'NUMBER', 'Temperature', 'LLM temperature 参数', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::llm.maxTokens', 1, NULL, NULL, NULL, NULL, NULL, 'llm.maxTokens', '4000', 'NUMBER', 'Max Tokens', 'LLM 最大输出 token 数', '[{"type":"min","value":"100"},{"type":"max","value":"32000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::llm.timeoutMs', 1, NULL, NULL, NULL, NULL, NULL, 'llm.timeoutMs', '60000', 'NUMBER', 'LLM 超时', 'LLM 请求超时时间（毫秒）', '[{"type":"min","value":"5000"},{"type":"max","value":"300000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- JWT 配置
  ('1::::::jwt.secret', 1, NULL, NULL, NULL, NULL, NULL, 'jwt.secret', 'adeptify-256-bit-secret-key-for-jwt-signing', 'STRING', 'JWT Secret', 'JWT 签名密钥', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::jwt.accessTokenValidity', 1, NULL, NULL, NULL, NULL, NULL, 'jwt.accessTokenValidity', '7200', 'NUMBER', 'Access Token 有效期', 'Access Token 有效期（秒）', '[{"type":"min","value":"60"},{"type":"max","value":"86400"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::jwt.refreshTokenValidity', 1, NULL, NULL, NULL, NULL, NULL, 'jwt.refreshTokenValidity', '604800', 'NUMBER', 'Refresh Token 有效期', 'Refresh Token 有效期（秒）', '[{"type":"min","value":"3600"},{"type":"max","value":"2592000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Character 默认配置
  ('1::::::character.maxSteps', 1, NULL, NULL, NULL, NULL, NULL, 'character.maxSteps', '10', 'NUMBER', '最大步数', 'Character 执行最大步数', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::character.maxIterations', 1, NULL, NULL, NULL, NULL, NULL, 'character.maxIterations', '10', 'NUMBER', '最大迭代次数', 'ReAct 执行最大迭代次数', '[{"type":"min","value":"1"},{"type":"max","value":"50"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::character.enableMemorySearch', 1, NULL, NULL, NULL, NULL, NULL, 'character.enableMemorySearch', 'true', 'BOOLEAN', '启用记忆搜索', '是否启用记忆搜索功能', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::character.enableToolUse', 1, NULL, NULL, NULL, NULL, NULL, 'character.enableToolUse', 'true', 'BOOLEAN', '启用工具调用', '是否启用工具调用功能', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Skill 默认配置
  ('1::::::skill.maxSingleFileBytes', 1, NULL, NULL, NULL, NULL, NULL, 'skill.maxSingleFileBytes', '2097152', 'NUMBER', '最大单文件大小', '单文件最大字节数', '[{"type":"min","value":"1024"},{"type":"max","value":"104857600"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::skill.maxZipFileBytes', 1, NULL, NULL, NULL, NULL, NULL, 'skill.maxZipFileBytes', '10485760', 'NUMBER', '最大 ZIP 大小', 'ZIP 文件最大字节数', '[{"type":"min","value":"1024"},{"type":"max","value":"104857600"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::skill.maxFileCount', 1, NULL, NULL, NULL, NULL, NULL, 'skill.maxFileCount', '100', 'NUMBER', '最大文件数', '解压最大文件数量', '[{"type":"min","value":"1"},{"type":"max","value":"1000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::skill.askTimeoutMs', 1, NULL, NULL, NULL, NULL, NULL, 'skill.askTimeoutMs', '30000', 'NUMBER', 'Skill 超时', 'Skill 请求超时（毫秒）', '[{"type":"min","value":"5000"},{"type":"max","value":"120000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Memory 默认配置
  ('1::::::memory.similarityThreshold', 1, NULL, NULL, NULL, NULL, NULL, 'memory.similarityThreshold', '0.7', 'NUMBER', '相似度阈值', '向量搜索相似度阈值', '[{"type":"min","value":"0"},{"type":"max","value":"1"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Sandbox 配置
  ('1::::::sandbox.workspaceRoot', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.workspaceRoot', '~/.dragon/sandboxes', 'STRING', '沙箱工作目录', '沙箱工作空间根目录', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::sandbox.dockerImage', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.dockerImage', 'dragonhead-sandbox', 'STRING', 'Docker 镜像', '沙箱 Docker 镜像', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::sandbox.idleHours', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.idleHours', '24', 'NUMBER', '空闲时间', '沙箱空闲小时后自动清理', '[{"type":"min","value":"1"},{"type":"max","value":"168"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::sandbox.maxAgeDays', 1, NULL, NULL, NULL, NULL, NULL, 'sandbox.maxAgeDays', '7', 'NUMBER', '最大保留天数', '沙箱最大保留天数', '[{"type":"min","value":"1"},{"type":"max","value":"30"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Web 工具配置
  ('1::::::web.maxBodyChars', 1, NULL, NULL, NULL, NULL, NULL, 'web.maxBodyChars', '50000', 'NUMBER', '最大响应字符', 'Web 请求最大响应字符数', '[{"type":"min","value":"1000"},{"type":"max","value":"500000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::web.requestTimeout', 1, NULL, NULL, NULL, NULL, NULL, 'web.requestTimeout', '30', 'NUMBER', '请求超时', 'Web 请求超时（秒）', '[{"type":"min","value":"5"},{"type":"max","value":"120"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Observer 配置
  ('1::::::observer.optimizationThreshold', 1, NULL, NULL, NULL, NULL, NULL, 'observer.optimizationThreshold', '0.6', 'NUMBER', '优化阈值', '观察者优化建议阈值', '[{"type":"min","value":"0"},{"type":"max","value":"1"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::observer.consecutiveLowScoreThreshold', 1, NULL, NULL, NULL, NULL, NULL, 'observer.consecutiveLowScoreThreshold', '3', 'NUMBER', '低分连续次数', '连续低分触发优化次数', '[{"type":"min","value":"1"},{"type":"max","value":"10"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::observer.periodicEvaluationHours', 1, NULL, NULL, NULL, NULL, NULL, 'observer.periodicEvaluationHours', '24', 'NUMBER', '评估周期', '定期评估周期（小时）', '[{"type":"min","value":"1"},{"type":"max","value":"168"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::observer.manualApprovalRequired', 1, NULL, NULL, NULL, NULL, NULL, 'observer.manualApprovalRequired', 'true', 'BOOLEAN', '需要手动审批', '优化计划是否需要手动审批', NULL, NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- Workspace 默认配置
  ('1::::::workspace.workingStyle', 1, NULL, NULL, NULL, NULL, NULL, 'workspace.workingStyle', 'COLLABORATIVE', 'STRING', '工作风格', '工作空间默认工作风格', NULL, '["AGGRESSIVE","CONSERVATIVE","COLLABORATIVE","INNOVATIVE","ANALYTICAL"]', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::workspace.decisionPattern', 1, NULL, NULL, NULL, NULL, NULL, 'workspace.decisionPattern', 'CONSULTATIVE', 'STRING', '决策模式', '工作空间默认决策模式', NULL, '["DEMOCRATIC","AUTOCRATIC","CONSENSUS","CONSULTATIVE"]', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::workspace.riskTolerance', 1, NULL, NULL, NULL, NULL, NULL, 'workspace.riskTolerance', '0.5', 'NUMBER', '风险容忍度', '工作空间风险容忍度', '[{"type":"min","value":"0"},{"type":"max","value":"1"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  -- 系统配置
  ('1::::::system.maxLoginFailCount', 1, NULL, NULL, NULL, NULL, NULL, 'system.maxLoginFailCount', '5', 'NUMBER', '最大登录失败次数', '登录失败锁定次数', '[{"type":"min","value":"3"},{"type":"max","value":"10"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::system.lockMinutes', 1, NULL, NULL, NULL, NULL, NULL, 'system.lockMinutes', '15', 'NUMBER', '锁定时间', '登录失败锁定时间（分钟）', '[{"type":"min","value":"5"},{"type":"max","value":"60"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::system.smsCodeValidity', 1, NULL, NULL, NULL, NULL, NULL, 'system.smsCodeValidity', '5', 'NUMBER', '短信验证码有效期', '短信验证码有效期（分钟）', '[{"type":"min","value":"1"},{"type":"max","value":"30"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  ('1::::::system.smsSendCooldown', 1, NULL, NULL, NULL, NULL, NULL, 'system.smsSendCooldown', '60', 'NUMBER', '短信发送冷却', '短信发送冷却时间（秒）', '[{"type":"min","value":"30"},{"type":"max","value":"300"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  ('1::::::exec.defaultTimeout', 1, NULL, NULL, NULL, NULL, NULL, 'exec.defaultTimeout', '120', 'NUMBER', '执行超时', '命令执行默认超时（秒）', '[{"type":"min","value":"10"},{"type":"max","value":"3600"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  ('1::::::bash.defaultMaxOutputChars', 1, NULL, NULL, NULL, NULL, NULL, 'bash.defaultMaxOutputChars', '50000', 'NUMBER', 'Bash 输出限制', 'Bash 命令最大输出字符数', '[{"type":"min","value":"1000"},{"type":"max","value":"500000"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  ('1::::::filetools.maxMatches', 1, NULL, NULL, NULL, NULL, NULL, 'filetools.maxMatches', '50', 'NUMBER', '文件搜索限制', '文件搜索最大匹配数', '[{"type":"min","value":"1"},{"type":"max","value":"500"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),

  ('1::::::tool.maxDetailEntries', 1, NULL, NULL, NULL, NULL, NULL, 'tool.maxDetailEntries', '8', 'NUMBER', '工具详情条目', '工具详情最大条目数', '[{"type":"min","value":"1"},{"type":"max","value":"50"}]', NULL, 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_WORKSPACE (scopeBit=5) ws-demo 工作空间级别配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('5:ws-demo:::::llm.temperature', 5, 'ws-demo', NULL, NULL, NULL, NULL, 'llm.temperature', '0.8', 'NUMBER', 'Temperature', 'ws-demo 默认 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('5:ws-demo:::::llm.maxTokens', 5, 'ws-demo', NULL, NULL, NULL, NULL, 'llm.maxTokens', '6000', 'NUMBER', 'Max Tokens', 'ws-demo 增加输出长度', '[{"type":"min","value":"100"},{"type":"max","value":"32000"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('5:ws-demo:::::workspace.workingStyle', 5, 'ws-demo', NULL, NULL, NULL, NULL, 'workspace.workingStyle', 'COLLABORATIVE', 'STRING', '工作风格', 'ws-demo 工作风格', NULL, '["AGGRESSIVE","CONSERVATIVE","COLLABORATIVE","INNOVATIVE","ANALYTICAL"]', 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('5:ws-demo:::::workspace.maxMembers', 5, 'ws-demo', NULL, NULL, NULL, NULL, 'workspace.maxMembers', '10', 'NUMBER', '最大成员数', 'ws-demo 最大成员数量', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_CHARACTER (scopeBit=9) 角色级别配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('9::char-001::::llm.temperature', 9, NULL, 'char-001', NULL, NULL, NULL, 'llm.temperature', '0.9', 'NUMBER', 'Temperature', 'char-001 默认 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('9::char-001::::character.maxSteps', 9, NULL, 'char-001', NULL, NULL, NULL, 'character.maxSteps', '15', 'NUMBER', '最大步数', 'char-001 增加最大步数', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('9::char-001::::character.systemPrompt', 9, NULL, 'char-001', NULL, NULL, NULL, 'character.systemPrompt', '你是一位专业的 AI 项目管理者。你的职责是协调团队工作、跟踪进度、识别风险。', 'STRING', 'System Prompt', 'char-001 系统提示词', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('9::char-002::::llm.temperature', 9, NULL, 'char-002', NULL, NULL, NULL, 'llm.temperature', '0.6', 'NUMBER', 'Temperature', 'char-002 默认 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('9::char-002::::character.maxSteps', 9, NULL, 'char-002', NULL, NULL, NULL, 'character.maxSteps', '8', 'NUMBER', '最大步数', 'char-002 限制步数', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('9::char-003::::llm.temperature', 9, NULL, 'char-003', NULL, NULL, NULL, 'llm.temperature', '0.7', 'NUMBER', 'Temperature', 'char-003 默认 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('9::char-003::::character.systemPrompt', 9, NULL, 'char-003', NULL, NULL, NULL, 'character.systemPrompt', '你是一位热情的客服代表，始终以客户满意为首要目标。', 'STRING', 'System Prompt', 'char-003 系统提示词', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_WS_CHAR (scopeBit=13) ws-demo + char-001 配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('13:ws-demo:char-001::::llm.temperature', 13, 'ws-demo', 'char-001', NULL, NULL, NULL, 'llm.temperature', '0.85', 'NUMBER', 'Temperature', 'ws-demo 下 char-001 的特化 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('13:ws-test:char-001::::llm.temperature', 13, 'ws-test', 'char-001', NULL, NULL, NULL, 'llm.temperature', '0.72', 'NUMBER', 'Temperature', 'ws-test 下 char-001 的特化 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('13:ws-demo:char-001::::llm.maxTokens', 13, 'ws-demo', 'char-001', NULL, NULL, NULL, 'llm.maxTokens', '8000', 'NUMBER', 'Max Tokens', 'ws-demo 下 char-001 增加输出', '[{"type":"min","value":"100"},{"type":"max","value":"32000"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('13:ws-demo:char-001::::character.maxIterations', 13, 'ws-demo', 'char-001', NULL, NULL, NULL, 'character.maxIterations', '15', 'NUMBER', '最大迭代', 'ws-demo 下 char-001 增加迭代', '[{"type":"min","value":"1"},{"type":"max","value":"50"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('13:ws-demo:char-001::::workspace.specialMode', 13, 'ws-demo', 'char-001', NULL, NULL, NULL, 'workspace.specialMode', 'true', 'BOOLEAN', '特殊模式', 'ws-demo 下 char-001 启用特殊模式', NULL, NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),

  ('13:ws-demo:char-002::::llm.temperature', 13, 'ws-demo', 'char-002', NULL, NULL, NULL, 'llm.temperature', '0.65', 'NUMBER', 'Temperature', 'ws-demo 下 char-002 的特化 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('13:ws-demo:char-002::::character.enableMemorySearch', 13, 'ws-demo', 'char-002', NULL, NULL, NULL, 'character.enableMemorySearch', 'true', 'BOOLEAN', '启用记忆搜索', 'ws-demo 下 char-002 启用记忆搜索', NULL, NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),

  ('13:ws-demo:char-003:llm.temperature', 13, 'ws-demo', 'char-003', NULL, NULL, NULL, 'llm.temperature', '0.75', 'NUMBER', 'Temperature', 'ws-demo 下 char-003 的特化 temperature', '[{"type":"min","value":"0"},{"type":"max","value":"2"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_SKILL (scopeBit=17) 技能级别配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('17::::skill-001::skill.priority', 17, NULL, NULL, NULL, 'skill-001', NULL, 'skill.priority', '10', 'NUMBER', '优先级', 'skill-001 默认优先级', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('17::::skill-001::skill.enabled', 17, NULL, NULL, NULL, 'skill-001', NULL, 'skill.enabled', 'true', 'BOOLEAN', '启用状态', 'skill-001 是否启用', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('17::::skill-001::skill.maxRetries', 17, NULL, NULL, NULL, 'skill-001', NULL, 'skill.maxRetries', '3', 'NUMBER', '最大重试', 'skill-001 最大重试次数', '[{"type":"min","value":"0"},{"type":"max","value":"10"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('17::::skill-002:skill.priority', 17, NULL, NULL, NULL, 'skill-002', NULL, 'skill.priority', '8', 'NUMBER', '优先级', 'skill-002 默认优先级', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('17::::skill-002:skill.timeout', 17, NULL, NULL, NULL, 'skill-002', NULL, 'skill.timeout', '60000', 'NUMBER', '超时时间', 'skill-002 超时时间（毫秒）', '[{"type":"min","value":"5000"},{"type":"max","value":"300000"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('17::::skill-data-analysis::skill.priority', 17, NULL, NULL, NULL, 'skill-data-analysis', NULL, 'skill.priority', '5', 'NUMBER', '优先级', 'skill-data-analysis 默认优先级', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('17::::skill-data-analysis::skill.model', 17, NULL, NULL, NULL, 'skill-data-analysis', NULL, 'skill.model', 'model-deepseek', 'STRING', '专用模型', 'skill-data-analysis 专用模型', NULL, '["model-kimi", "model-deepseek"]', 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_WS_SKILL (scopeBit=21) ws-demo + skill 配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('21:ws-demo:::skill-001::skill.priority', 21, 'ws-demo', NULL, NULL, 'skill-001', NULL, 'skill.priority', '12', 'NUMBER', '优先级', 'ws-demo 下 skill-001 调整优先级', '[{"type":"min","value":"1"},{"type":"max","value":"100"}]', NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('21:ws-demo:::skill-001::skill.enabled', 21, 'ws-demo', NULL, NULL, 'skill-001', NULL, 'skill.enabled', 'true', 'BOOLEAN', '启用状态', 'ws-demo 下 skill-001 是否启用', NULL, NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_TOOL (scopeBit=33) 工具级别配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('33:::tool-001:::tool.timeout', 33, NULL, NULL, 'tool-001', NULL, NULL, 'tool.timeout', '30000', 'NUMBER', '超时时间', 'tool-001 超时时间（毫秒）', '[{"type":"min","value":"1000"},{"type":"max","value":"300000"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('33:::tool-001:::tool.retryCount', 33, NULL, NULL, 'tool-001', NULL, NULL, 'tool.retryCount', '3', 'NUMBER', '重试次数', 'tool-001 重试次数', '[{"type":"min","value":"0"},{"type":"max","value":"10"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('33:::tool-code-review:::tool.timeout', 33, NULL, NULL, 'tool-code-review', NULL, NULL, 'tool.timeout', '60000', 'NUMBER', '超时时间', 'tool-code-review 超时时间', '[{"type":"min","value":"1000"},{"type":"max","value":"300000"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('33:::tool-code-review:::tool.maxFileSize', 33, NULL, NULL, 'tool-code-review', NULL, NULL, 'tool.maxFileSize', '1048576', 'NUMBER', '最大文件', 'tool-code-review 最大文件大小', '[{"type":"min","value":"1024"},{"type":"max","value":"10485760"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('33:::tool-data-export:::tool.timeout', 33, NULL, NULL, 'tool-data-export', NULL, NULL, 'tool.timeout', '120000', 'NUMBER', '超时时间', 'tool-data-export 超时时间', '[{"type":"min","value":"1000"},{"type":"max","value":"300000"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('33:::tool-data-export:::tool.format', 33, NULL, NULL, 'tool-data-export', NULL, NULL, 'tool.format', 'json', 'STRING', '默认格式', 'tool-data-export 默认导出格式', NULL, '["json","csv","xlsx"]', 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== GLOBAL_MEMORY (scopeBit=65) 记忆级别配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('65:::::cs-001:memory.enabled', 65, NULL, NULL, NULL, NULL, 'cs-001', 'memory.enabled', 'true', 'BOOLEAN', '启用状态', 'cs-001 是否启用', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('65:::::cs-001:memory.maxChunks', 65, NULL, NULL, NULL, NULL, 'cs-001', 'memory.maxChunks', '1000', 'NUMBER', '最大分块', 'cs-001 最大分块数', '[{"type":"min","value":"1"},{"type":"max","value":"10000"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('65:::::cs-001:memory.retentionDays', 65, NULL, NULL, NULL, NULL, 'cs-001', 'memory.retentionDays', '30', 'NUMBER', '保留天数', 'cs-001 记忆保留天数', '[{"type":"min","value":"1"},{"type":"max","value":"365"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('65:::::cs-002:memory.enabled', 65, NULL, NULL, NULL, NULL, 'cs-002', 'memory.enabled', 'true', 'BOOLEAN', '启用状态', 'cs-002 是否启用', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('65:::::cs-002:memory.autoUpdate', 65, NULL, NULL, NULL, NULL, 'cs-002', 'memory.autoUpdate', 'true', 'BOOLEAN', '自动更新', 'cs-002 是否自动更新', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('65:::::cs-003:memory.enabled', 65, NULL, NULL, NULL, NULL, 'cs-003', 'memory.enabled', 'true', 'BOOLEAN', '启用状态', 'cs-003 是否启用', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('65:::::cs-003:memory.securityLevel', 65, NULL, NULL, NULL, NULL, 'cs-003', 'memory.securityLevel', 'high', 'STRING', '安全级别', 'cs-003 安全级别', NULL, '["low","medium","high"]', 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== OBSERVER 粒度配置 (scopeBit=133+) ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, validation_rules, options, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('133:::obs-001::observer.alertThreshold', 133, NULL, 'obs-001', NULL, NULL, NULL, 'observer.alertThreshold', '0.5', 'NUMBER', '告警阈值', 'obs-001 告警阈值', '[{"type":"min","value":"0"},{"type":"max","value":"1"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('133:::obs-001::observer.autoApproval', 133, NULL, 'obs-001', NULL, NULL, NULL, 'observer.autoApproval', 'false', 'BOOLEAN', '自动审批', 'obs-001 是否启用自动审批', NULL, NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),
  ('133:::obs-001::observer.maxPlansPerDay', 133, NULL, 'obs-001', NULL, NULL, NULL, 'observer.maxPlansPerDay', '5', 'NUMBER', '每日最大计划', 'obs-001 每日最大生成计划数', '[{"type":"min","value":"1"},{"type":"max","value":"50"}]', NULL, 'PUBLISHED', 1, 'owner', 'owner', NOW(), NOW(), NOW()),

  ('137:::obs-001:ws-demo:observer.enabled', 137, 'ws-demo', 'obs-001', NULL, NULL, NULL, 'observer.enabled', 'true', 'BOOLEAN', '启用状态', 'ws-demo 下 obs-001 是否启用', NULL, NULL, 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW()),
  ('137:::obs-001:ws-demo:observer.evaluationMode', 137, 'ws-demo', 'obs-001', NULL, NULL, NULL, 'observer.evaluationMode', 'auto', 'STRING', '评估模式', 'ws-demo 下 obs-001 评估模式', NULL, '["auto","manual","plan_approval"]', 'PUBLISHED', 1, 'admin', 'admin', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ==================== Prompt 配置 ====================
INSERT INTO config_store (id, scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key, config_value, value_type, name, description, status, version, modified_by, published_by, published_at, created_at, updated_at)
VALUES
  ('1::::::prompt.character.system', 1, NULL, NULL, NULL, NULL, NULL, 'prompt.character.system', '你是一个专业的AI数字员工，有自己的性格特点和价值观。', 'STRING', 'Character 系统提示词', '全局默认 Character 系统提示词', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::prompt.character.task', 1, NULL, NULL, NULL, NULL, NULL, 'prompt.character.task', '请根据要求完成以下任务：', 'STRING', 'Character 任务提示词', '全局默认任务提示词', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::prompt.react.execute', 1, NULL, NULL, NULL, NULL, NULL, 'prompt.react.execute', '你是 ReAct 执行阶段的 PromptWriter，需要基于给定的模板、任务上下文、历史思考、动作、观察结果和工具信息，生成当前这一轮真正给执行模型使用的提示词。', 'STRING', 'ReAct 执行 Prompt', 'ReAct 执行阶段提示词', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::prompt.react.taskDecompose', 1, NULL, NULL, NULL, NULL, NULL, 'prompt.react.taskDecompose', '你是一个组织调度专家，负责把复杂任务拆解为可执行的子任务。', 'STRING', '任务拆解 Prompt', '任务拆解提示词', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW()),
  ('1::::::prompt.observer.suggestion', 1, NULL, NULL, NULL, NULL, NULL, 'prompt.observer.suggestion', '# Observer LLM Optimization Suggestion Prompt\n\n## Role\n你是一个专业的AI优化顾问...', 'STRING', 'Observer 建议 Prompt', '观察者优化建议提示词', 'PUBLISHED', 1, 'system', 'system', NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = NOW();

-- ============================================================================
-- 验证查询
-- ============================================================================
-- SELECT '=== Workspace ===' AS info; SELECT * FROM workspace;
-- SELECT '=== Character ===' AS info; SELECT * FROM `character`;
-- SELECT '=== Trait ===' AS info; SELECT * FROM trait;
-- SELECT '=== Skills ===' AS info; SELECT * FROM skills;
-- SELECT '=== Tool ===' AS info; SELECT * FROM tool;
-- SELECT '=== Observer ===' AS info; SELECT * FROM observer;
-- SELECT '=== Task ===' AS info; SELECT * FROM task WHERE workspace_id = 'ws-demo';
-- SELECT '=== Notification ===' AS info; SELECT * FROM notification WHERE user_id = 1;
-- SELECT '=== Asset Member ===' AS info; SELECT * FROM asset_member WHERE user_id = 1;
-- SELECT '=== ConfigStore ===' AS info; SELECT id, scope_bit, config_key, config_value FROM config_store ORDER BY scope_bit;
