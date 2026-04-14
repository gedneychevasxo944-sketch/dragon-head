-- ============================================================================
-- V105: 插入 Character 专用内置工具数据
-- 包含 hr / observer / prompt_writer 三类 Character 的专属工具，
-- 工具类型均为 ATOMIC，通过 execution_config.className 指向具体实现类。
-- ============================================================================

-- ============================================================================
-- 1. tool 主表（11 个 character 工具）
-- ============================================================================
INSERT INTO tool (id, name, introduction, tool_type, visibility, builtin, tags, creator_type, status, created_at) VALUES
-- hr
('builtin-assign-duty',       'AssignDuty',              'Assign a duty description to a character in a workspace',                                          'ATOMIC', 'PUBLIC', 1, '["hr","职责","Character"]',         'OFFICIAL', 'ACTIVE', NOW()),
('builtin-fire-character',    'FireCharacter',            'Fire a character from a workspace',                                                                'ATOMIC', 'PUBLIC', 1, '["hr","解雇","Character"]',         'OFFICIAL', 'ACTIVE', NOW()),
('builtin-hire-character',    'HireCharacter',            'Hire a character to a workspace',                                                                  'ATOMIC', 'PUBLIC', 1, '["hr","雇佣","Character"]',         'OFFICIAL', 'ACTIVE', NOW()),
('builtin-list-candidates',   'ListCandidates',           'List available character candidates for a workspace',                                              'ATOMIC', 'PUBLIC', 1, '["hr","候选人","Character"]',       'OFFICIAL', 'ACTIVE', NOW()),
('builtin-evaluate-character','EvaluateCharacter',        'Evaluate a character''s suitability for a workspace',                                              'ATOMIC', 'PUBLIC', 1, '["hr","评估","Character"]',         'OFFICIAL', 'ACTIVE', NOW()),
-- observer
('builtin-get-char-state',    'GetCharacterState',        'Get the current state of a character',                                                             'ATOMIC', 'PUBLIC', 1, '["observer","状态","Character"]',   'OFFICIAL', 'ACTIVE', NOW()),
('builtin-get-ws-state',      'GetWorkspaceState',        'Get the current state of a workspace',                                                             'ATOMIC', 'PUBLIC', 1, '["observer","状态","Workspace"]',   'OFFICIAL', 'ACTIVE', NOW()),
('builtin-get-recent-tasks',  'GetRecentTasks',           'Get recent task execution data',                                                                   'ATOMIC', 'PUBLIC', 1, '["observer","任务","数据"]',        'OFFICIAL', 'ACTIVE', NOW()),
('builtin-get-eval-records',  'GetEvaluationRecords',     'Get historical evaluation records',                                                                'ATOMIC', 'PUBLIC', 1, '["observer","评估","记录"]',        'OFFICIAL', 'ACTIVE', NOW()),
('builtin-explore-obs-needs', 'ExploreObservationNeeds',  'Analyze which data collection tools are needed based on the request',                              'ATOMIC', 'PUBLIC', 1, '["observer","分析","观测"]',        'OFFICIAL', 'ACTIVE', NOW()),
-- prompt_writer
('builtin-get-ws-commonsense','GetWorkspaceCommonSense',  'Get the CommonSense Prompt for a workspace',                                                       'ATOMIC', 'PUBLIC', 1, '["prompt","常识","Workspace"]',     'OFFICIAL', 'ACTIVE', NOW());

-- ============================================================================
-- 2. tool_version 版本表
-- ============================================================================

-- ── assign_duty ───────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-assign-duty', 1, 'assign_duty',
  'Assign a duty description to a character in a workspace. Use this tool to define what the character should do.',
  '{
    "workspaceId":    {"type": "string", "description": "The workspace ID"},
    "characterId":    {"type": "string", "description": "The character ID"},
    "dutyDescription":{"type": "string", "description": "The duty description for the character"}
  }',
  '["workspaceId", "characterId", "dutyDescription"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.hr.AssignDutyTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── fire_character ────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-fire-character', 1, 'fire_character',
  'Fire a character from a workspace. Use this tool when you need to remove a character from your team.',
  '{
    "workspaceId": {"type": "string", "description": "The workspace ID to fire the character from"},
    "characterId": {"type": "string", "description": "The character ID to fire"}
  }',
  '["workspaceId", "characterId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.hr.FireCharacterTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── hire_character ────────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-hire-character', 1, 'hire_character',
  'Hire a character to a workspace. Use this tool when you need to add a character to your team.',
  '{
    "workspaceId": {"type": "string", "description": "The workspace ID to hire the character into"},
    "characterId": {"type": "string", "description": "The character ID to hire"}
  }',
  '["workspaceId", "characterId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.hr.HireCharacterTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── list_candidates ───────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-list-candidates', 1, 'list_candidates',
  'List available character candidates for a workspace. This shows characters that are not yet members of the workspace.',
  '{
    "workspaceId": {"type": "string", "description": "The workspace ID to list candidates for"}
  }',
  '["workspaceId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.hr.ListCandidatesTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── evaluate_character ────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-evaluate-character', 1, 'evaluate_character',
  'Evaluate a character''s suitability for a workspace. Returns character information and compatibility assessment.',
  '{
    "characterId": {"type": "string", "description": "The character ID to evaluate"},
    "workspaceId": {"type": "string", "description": "The workspace ID to evaluate against"}
  }',
  '["characterId", "workspaceId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.hr.EvaluateCharacterTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── get_character_state ───────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-get-char-state', 1, 'get_character_state',
  '获取 Character 的当前状态，包括基本信息、任务统计、技能标签等',
  '{
    "characterId": {"type": "string", "description": "Character ID"}
  }',
  '["characterId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.observer.GetCharacterStateTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── get_workspace_state ───────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-get-ws-state', 1, 'get_workspace_state',
  '获取 Workspace 的当前状态，包括成员列表、任务统计、配置信息等',
  '{
    "workspaceId": {"type": "string", "description": "Workspace ID"}
  }',
  '["workspaceId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.observer.GetWorkspaceStateTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── get_recent_tasks ──────────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-get-recent-tasks', 1, 'get_recent_tasks',
  '获取最近的任务执行数据，包括任务输入、输出、耗时、成功率等',
  '{
    "targetType": {"type": "string",  "description": "目标类型：CHARACTER 或 WORKSPACE", "enumValues": ["CHARACTER", "WORKSPACE"]},
    "targetId":   {"type": "string",  "description": "目标 ID"},
    "days":       {"type": "integer", "description": "收集最近多少天的数据，默认 7 天", "default": 7}
  }',
  '["targetType", "targetId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.observer.GetRecentTasksTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── get_evaluation_records ────────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-get-eval-records', 1, 'get_evaluation_records',
  '获取历史评估记录，包括评分、发现的问题、改进建议等',
  '{
    "targetType": {"type": "string",  "description": "目标类型：CHARACTER 或 WORKSPACE", "enumValues": ["CHARACTER", "WORKSPACE"]},
    "targetId":   {"type": "string",  "description": "目标 ID"},
    "days":       {"type": "integer", "description": "收集最近多少天的数据，默认 30 天", "default": 30}
  }',
  '["targetType", "targetId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.observer.GetEvaluationRecordsTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── explore_observation_needs ─────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-explore-obs-needs', 1, 'explore_observation_needs',
  '分析用户请求，确定需要调用哪些数据获取工具来收集观测数据',
  '{
    "userPrompt": {"type": "string", "description": "用户的请求 prompt"},
    "targetType": {"type": "string", "description": "目标类型：CHARACTER 或 WORKSPACE", "enumValues": ["CHARACTER", "WORKSPACE"]},
    "targetId":   {"type": "string", "description": "目标 ID"}
  }',
  '["targetType", "targetId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.observer.ExploreObservationNeedsTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ── get_workspace_common_sense ────────────────────────────────────────────────
INSERT INTO tool_version (tool_id, version, name, description, parameters, required_params, aliases, execution_config, tool_type, status, published_at, created_at) VALUES
(
  'builtin-get-ws-commonsense', 1, 'get_workspace_common_sense',
  '获取指定 Workspace 的 CommonSense Prompt。当需要了解工作空间的常识规则、约束条件、角色边界或长期目标时调用此工具。',
  '{
    "workspaceId": {"type": "string", "description": "工作空间 ID"}
  }',
  '["workspaceId"]',
  NULL,
  '{"className": "org.dragon.tool.runtime.tools.character.prompt_writer.GetWorkspaceCommonSenseTool"}',
  'ATOMIC', 'PUBLISHED', NOW(), NOW()
);

-- ============================================================================
-- 3. 回填 tool.published_version_id
-- ============================================================================
UPDATE tool t
JOIN tool_version v ON v.tool_id = t.id AND v.version = 1
SET t.published_version_id = v.id
WHERE t.builtin = 1
  AND t.id IN (
    'builtin-assign-duty', 'builtin-fire-character', 'builtin-hire-character',
    'builtin-list-candidates', 'builtin-evaluate-character',
    'builtin-get-char-state', 'builtin-get-ws-state', 'builtin-get-recent-tasks',
    'builtin-get-eval-records', 'builtin-explore-obs-needs',
    'builtin-get-ws-commonsense'
  );

