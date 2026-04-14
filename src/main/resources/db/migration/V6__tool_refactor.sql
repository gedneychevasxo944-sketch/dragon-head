-- ============================================================================
-- V6: Tool 模块重构
-- 旧 tool 表（name 主键）废弃，重建为与 ToolDO 对齐的 tool 表，
-- 并新增 tool_version / tool_binding / tool_action_log / tool_execution_record
-- ============================================================================

-- 旧表改名（保留数据，避免直接 DROP）
ALTER TABLE tool RENAME TO tool_legacy;

-- ============================================================================
-- tool 主表
-- ============================================================================
CREATE TABLE IF NOT EXISTS tool (
    id              VARCHAR(64)  PRIMARY KEY                           COMMENT '工具唯一 ID（UUID）',
    name            VARCHAR(128) NOT NULL                              COMMENT '工具名称，LLM 通过此名称发起 tool_call',
    introduction    TEXT                                               COMMENT '工具简介（管理页面展示用）',
    tool_type       VARCHAR(32)  NOT NULL                              COMMENT '工具类型：ATOMIC / HTTP / MCP / CODE / SKILL',
    visibility      VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE'           COMMENT '可见性：PUBLIC / WORKSPACE / PRIVATE',
    builtin         TINYINT(1)   NOT NULL DEFAULT 0                   COMMENT '是否为系统内置工具（1=是，0=否）',
    tags            JSON                                               COMMENT '标签列表（JSON 数组字符串，如 ["搜索","文件"]）',
    creator_type    VARCHAR(20)                                        COMMENT '创建者类型：PERSONAL / OFFICIAL',
    creator_id      BIGINT                                             COMMENT '创建者用户 ID',
    creator_name    VARCHAR(100)                                       COMMENT '创建者用户名',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'            COMMENT '工具状态：ACTIVE / INACTIVE / DEPRECATED',
    published_version_id BIGINT                                        COMMENT '已发布版本 ID（指向 tool_version.id）',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',
    deleted_at      DATETIME                                           COMMENT '删除时间（软删除，非 null 即已删除）',

    UNIQUE KEY uk_tool_name (name),
    INDEX idx_tool_type (tool_type),
    INDEX idx_tool_visibility (visibility),
    INDEX idx_tool_status (status),
    INDEX idx_tool_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具主表';

-- ============================================================================
-- tool_version 版本表
-- ============================================================================
CREATE TABLE IF NOT EXISTS tool_version (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT            COMMENT '版本自增主键',
    tool_id         VARCHAR(64)  NOT NULL                              COMMENT '所属工具 ID（关联 tool.id）',
    version         INT          NOT NULL                              COMMENT '版本号（从 1 开始，整数递增）',
    name            VARCHAR(128) NOT NULL                              COMMENT 'LLM tool_call 使用的工具名称',
    description     TEXT                                               COMMENT '工具描述（注入 LLM system prompt）',
    parameters      JSON                                               COMMENT '参数 Schema（JSON 格式，Map<String,ParameterSchema>）',
    required_params JSON                                               COMMENT '必填参数名列表（JSON 数组，如 ["command","path"]）',
    aliases         JSON                                               COMMENT '工具别名列表（JSON 数组，可为 null）',
    execution_config JSON                                              COMMENT '执行配置（结构因 tool_type 不同而异）',
    tool_type       VARCHAR(32)  NOT NULL                              COMMENT '工具类型（冗余字段，避免 JOIN tools）',
    editor_id       BIGINT                                             COMMENT '本次编辑者用户 ID',
    editor_name     VARCHAR(100)                                       COMMENT '本次编辑者用户名',
    storage_type    VARCHAR(20)                                        COMMENT '文件存储类型：LOCAL / S3',
    storage_info    JSON                                               COMMENT '文件存储元信息（ToolStorageInfoVO 的 JSON 序列化）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'             COMMENT '版本状态：DRAFT / PUBLISHED / DEPRECATED',
    release_note    TEXT                                               COMMENT '发版备注',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '版本创建时间',
    published_at    DATETIME                                           COMMENT '发布时间',

    UNIQUE KEY uk_tool_version (tool_id, version),
    INDEX idx_tool_version_tool (tool_id),
    INDEX idx_tool_version_status (tool_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具版本表';

-- ============================================================================
-- tool_binding 绑定表
-- ============================================================================
CREATE TABLE IF NOT EXISTS tool_binding (
    id              VARCHAR(64)  PRIMARY KEY                           COMMENT '绑定记录唯一 ID（UUID）',
    tool_id         VARCHAR(64)  NOT NULL                              COMMENT '被绑定工具的 ID（关联 tool.id）',
    scope_type      VARCHAR(20)  NOT NULL                              COMMENT '绑定范围：WORKSPACE / CHARACTER',
    workspace_id    VARCHAR(64)                                        COMMENT '绑定到的 workspace ID（scope_type=WORKSPACE 时有值）',
    character_id    VARCHAR(64)                                        COMMENT '绑定到的 character ID（scope_type=CHARACTER 时有值）',

    UNIQUE KEY uk_tool_binding (tool_id, scope_type, workspace_id, character_id),
    INDEX idx_tool_binding_workspace (workspace_id),
    INDEX idx_tool_binding_character (character_id),
    INDEX idx_tool_binding_tool (tool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具绑定表';

-- ============================================================================
-- tool_action_log 操作日志表
-- ============================================================================
CREATE TABLE IF NOT EXISTS tool_action_log (
    id              VARCHAR(64)  PRIMARY KEY                           COMMENT '主键 UUID',
    tool_id         VARCHAR(64)  NOT NULL                              COMMENT '工具 ID',
    tool_name       VARCHAR(128)                                       COMMENT '工具名称（冗余，便于展示）',
    action_type     VARCHAR(50)  NOT NULL                              COMMENT '动作类型',
    operator_id     BIGINT                                             COMMENT '操作人 ID',
    operator_name   VARCHAR(100)                                       COMMENT '操作人名称（冗余）',
    version         INT                                                COMMENT '涉及版本号（注册/更新/发布时有值）',
    detail          JSON                                               COMMENT '操作详情，结构因 action_type 而异',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP   COMMENT '创建时间',

    INDEX idx_tool_action_log (tool_id, action_type),
    INDEX idx_tool_action_log_time (tool_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具操作日志表';

-- ============================================================================
-- tool_execution_record 执行记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS tool_execution_record (
    execution_id        VARCHAR(64)  PRIMARY KEY                       COMMENT '执行记录 ID（主键）',
    tool_use_id         VARCHAR(128)                                   COMMENT '工具调用 ID（Claude API tool_use_id）',
    tool_name           VARCHAR(128) NOT NULL                          COMMENT '工具名称',
    tenant_id           VARCHAR(64)                                    COMMENT '租户 ID',
    session_id          VARCHAR(128)                                   COMMENT '会话 ID',
    agent_id            VARCHAR(64)                                    COMMENT '代理 ID（子代理时有值）',
    parent_execution_id VARCHAR(64)                                    COMMENT '父执行 ID（嵌套调用时有值）',
    message_id          VARCHAR(128)                                   COMMENT '关联消息 ID',
    status              VARCHAR(20)  NOT NULL DEFAULT 'RUNNING'        COMMENT '执行状态：RUNNING / SUCCESS / FAILED / TIMEOUT / CANCELLED',
    input_params        JSON                                           COMMENT '输入参数（JSON）',
    output_summary      TEXT                                           COMMENT '执行结果摘要（大结果仅存摘要，完整结果存 storage）',
    storage_type        VARCHAR(20)                                    COMMENT '结果存储类型：INLINE / LOCAL / S3 / REDIS',
    storage_key         VARCHAR(512)                                   COMMENT '结果存储 key（非 INLINE 时有值）',
    error_message       TEXT                                           COMMENT '错误信息（status=FAILED 时有值）',
    duration_ms         BIGINT                                         COMMENT '执行耗时（毫秒）',
    started_at          DATETIME                                       COMMENT '开始时间',
    finished_at         DATETIME                                       COMMENT '结束时间',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',

    INDEX idx_exec_record_tool (tool_name),
    INDEX idx_exec_record_session (session_id),
    INDEX idx_exec_record_status (status),
    INDEX idx_exec_record_agent (agent_id),
    INDEX idx_exec_record_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具执行记录表';

