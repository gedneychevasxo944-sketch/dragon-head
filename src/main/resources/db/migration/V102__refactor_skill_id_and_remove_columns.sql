-- V102: 技能相关表统一结构
-- 合并 V100, V101, V102 的所有变更
-- 1. id 改为 String 类型 (VARCHAR(64)) 作为业务主键，移除 BIGINT AUTO_INCREMENT
-- 2. 移除 skill_id 字段（已与 id 合并）
-- 3. 移除 display_name 字段
-- 4. 移除 comment 字段
-- 5. 添加 tags 列（原 V100）
-- 6. 添加 skill_action_log 表（原 V101）

-- 先删除旧数据（因为 id 类型变更需要重建表）
DROP TABLE IF EXISTS skill_usage_log;
DROP TABLE IF EXISTS skill_action_log;
DROP TABLE IF EXISTS skill_binding;
DROP TABLE IF EXISTS skill_version;
DROP TABLE IF EXISTS skill;

-- 重建 skill 表
CREATE TABLE skill (
    id                       VARCHAR(64) PRIMARY KEY COMMENT '技能ID（业务主键）',
    name                     VARCHAR(100) NOT NULL COMMENT '技能名称（管理页面显示用）',
    introduction             TEXT COMMENT '技能简介（管理页面展示用）',
    category                 VARCHAR(50)  COMMENT '技能功能分类（conversation/coder/data_analysis 等，不含 builtin）',
    visibility               VARCHAR(20)  NOT NULL DEFAULT 'private' COMMENT '可见性（public/private/workspace）',
    builtin                  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否为系统内置技能（1=内置，默认对所有 agent 可用；0=普通）',
    tags                     JSON COMMENT '标签列表',

    -- 创建者
    creator_type             VARCHAR(10)  NOT NULL DEFAULT 'personal' COMMENT '创建者类型（personal/official）',
    creator_id               BIGINT UNSIGNED COMMENT '创建者用户ID',
    creator_name             VARCHAR(100) COMMENT '创建者用户名',

    -- 状态和版本指针
    status                   VARCHAR(10)  NOT NULL DEFAULT 'draft' COMMENT '当前状态（draft/active/disabled）',
    published_version_id      BIGINT UNSIGNED COMMENT '已发布的版本ID（指向skill_version.id）',

    -- 时间戳
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted_at              DATETIME     DEFAULT NULL COMMENT '删除时间（软删除标记）',

    INDEX idx_creator (creator_id),
    INDEX idx_status (status),
    INDEX idx_name (name),
    INDEX idx_category (category),
    INDEX idx_builtin (builtin),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能信息表（不含版本内容）';

-- 重建 skill_version 表（skill_id 引用改为 skill.id VARCHAR(64)）
CREATE TABLE skill_version (
    id                       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '物理主键',
    skill_id                 VARCHAR(64)  NOT NULL COMMENT '所属技能ID',
    version                  INT UNSIGNED NOT NULL COMMENT '版本号（从1开始）',

    -- 版本内容（从 frontmatter 解析）
    name                     VARCHAR(100) COMMENT '版本名称（解析自 SKILL.md）',
    description              TEXT COMMENT '版本描述（解析自 SKILL.md）',
    content                  TEXT COMMENT 'SKILL.md 正文内容',
    frontmatter              TEXT COMMENT 'SKILL.md 原始 frontmatter YAML',
    runtime_config           JSON COMMENT '运行时配置JSON',

    -- 编辑者
    editor_id                BIGINT UNSIGNED COMMENT '本次编辑者用户ID',
    editor_name              VARCHAR(100) COMMENT '本次编辑者用户名',

    -- 版本状态
    status                   VARCHAR(10)  NOT NULL DEFAULT 'draft' COMMENT '版本状态（draft/published/deprecated）',
    release_note             TEXT COMMENT '发版备注',

    -- 存储信息
    storage_type             VARCHAR(10)  NOT NULL DEFAULT 'local' COMMENT '存储类型（local/s3）',
    storage_info             JSON COMMENT '存储信息（JSON结构）',

    -- 时间戳
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
    published_at             DATETIME     DEFAULT NULL COMMENT '发布时间',

    UNIQUE KEY uk_skill_version (skill_id, version),
    INDEX idx_skill_version_skill (skill_id),
    INDEX idx_skill_version_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能版本表（每次更新生成新版本）';

-- 重建 skill_binding 表
CREATE TABLE skill_binding (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',

    -- 绑定类型
    binding_type    VARCHAR(20)  NOT NULL COMMENT '绑定类型：character / workspace / character_workspace',

    -- 绑定主体（按 binding_type 选填）
    character_id    VARCHAR(64) DEFAULT NULL COMMENT 'Character 主键（binding_type=character 或 character_workspace 时填写）',
    workspace_id    VARCHAR(64) DEFAULT NULL COMMENT 'Workspace 主键（binding_type=workspace 或 character_workspace 时填写）',

    -- 绑定的 Skill
    skill_id        VARCHAR(64)  NOT NULL COMMENT '技能ID（UUID）',

    -- 时间戳
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_character (character_id),
    INDEX idx_workspace (workspace_id),
    INDEX idx_skill (skill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能绑定关系表';

-- 重建 skill_usage_log 表
CREATE TABLE skill_usage_log (
    id                       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    skill_id                 VARCHAR(64)  NOT NULL COMMENT '技能ID',
    skill_version            INT UNSIGNED COMMENT '本次执行命中的版本号',
    character_id             VARCHAR(64) COMMENT '使用人/角色ID',
    workspace_id             VARCHAR(64) COMMENT 'Workspace ID',
    channel                  VARCHAR(20) COMMENT '渠道',
    message_id               VARCHAR(64) COMMENT '消息ID',
    invoke_time              DATETIME     NOT NULL COMMENT '调用时间',

    INDEX idx_skill_id (skill_id),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_character_id (character_id),
    INDEX idx_invoke_time (invoke_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技能使用记录表';

-- 技能操作日志表
CREATE TABLE skill_action_log (
    id              VARCHAR(64) PRIMARY KEY COMMENT '主键 UUID',
    skill_id        VARCHAR(64) NOT NULL COMMENT '技能 UUID',
    skill_name      VARCHAR(100) COMMENT '技能名称（冗余，便于展示）',
    action_type     VARCHAR(50) NOT NULL COMMENT '动作类型',
    operator_id     BIGINT UNSIGNED COMMENT '操作人 ID',
    operator_name   VARCHAR(100) COMMENT '操作人名称（冗余）',
    version         INT UNSIGNED COMMENT '涉及版本号',
    detail          JSON COMMENT '操作详情，结构因 action_type 而异',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_skill_action (skill_id, action_type),
    INDEX idx_skill_time (skill_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 操作日志表';
