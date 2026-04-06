-- ============================================================================
-- Dragon Head 冷启动数据库 Schema
-- 整合自 V1~V10 所有迁移文件，冲突部分已处理
-- ============================================================================

use adeptify;

-- ============================================================================
-- V1: 核心业务表
-- ============================================================================

-- Chat message table
CREATE TABLE IF NOT EXISTS chat_message (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    sender_id VARCHAR(64) NOT NULL,
    receiver_id VARCHAR(64),
    content TEXT,
    message_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    session_id VARCHAR(64),
    timestamp DATETIME NOT NULL,
    `read` BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSON,
    task_id VARCHAR(64),
    parent_task_id VARCHAR(64),
    task_purpose VARCHAR(32),
    message_subtype VARCHAR(32),
    related_task_id VARCHAR(64),
    correlation_id VARCHAR(64),
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_session_id (session_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_task_id (task_id),
    INDEX idx_parent_task_id (parent_task_id),
    INDEX idx_correlation_id (correlation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chat session table
CREATE TABLE IF NOT EXISTS chat_session (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    participant_ids JSON,
    context JSON,
    participant_states JSON,
    task_states JSON,
    blocked_participants JSON,
    last_summary TEXT,
    decisions JSON,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    INDEX idx_session_workspace_id (workspace_id),
    INDEX idx_session_task_id (task_id),
    INDEX idx_session_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workspace table
CREATE TABLE IF NOT EXISTS workspace (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE',
    visibility VARCHAR(32) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC or PRIVATE',
    properties JSON,
    personality JSON,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_workspace_owner (owner),
    INDEX idx_workspace_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workspace member table
CREATE TABLE IF NOT EXISTS workspace_member (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    character_id VARCHAR(64) NOT NULL,
    role VARCHAR(64),
    layer VARCHAR(32) DEFAULT 'NORMAL',
    permission VARCHAR(32) NOT NULL DEFAULT 'VIEW' COMMENT 'VIEW, USE, EDIT, ADMIN, OWNER',
    tags JSON,
    weight DOUBLE DEFAULT 1.0,
    priority INT DEFAULT 0,
    reputation INT DEFAULT 0,
    resource_quota JSON,
    join_at DATETIME,
    last_active_at DATETIME,
    metadata JSON,
    INDEX idx_member_workspace (workspace_id),
    INDEX idx_member_character (character_id),
    UNIQUE KEY uk_workspace_character (workspace_id, character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Task table
CREATE TABLE IF NOT EXISTS task (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    parent_task_id VARCHAR(64),
    creator_id VARCHAR(64) NOT NULL,
    character_id VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input JSON,
    output JSON,
    result TEXT,
    error_message TEXT,
    child_task_ids JSON,
    collaboration_session_id VARCHAR(64),
    assigned_member_ids JSON,
    execution_steps JSON,
    execution_messages JSON,
    current_streaming_content TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    started_at DATETIME,
    completed_at DATETIME,
    execution_mode VARCHAR(32),
    workflow_id VARCHAR(64),
    dependency_task_ids JSON,
    waiting_reason VARCHAR(255),
    resume_token VARCHAR(64),
    resume_context JSON,
    source_message_id VARCHAR(64),
    source_chat_id VARCHAR(64),
    source_channel VARCHAR(32),
    material_ids JSON,
    last_question TEXT,
    interaction_context JSON,
    metadata JSON,
    extensions JSON,
    INDEX idx_task_workspace (workspace_id),
    INDEX idx_task_parent (parent_task_id),
    INDEX idx_task_character (character_id),
    INDEX idx_task_creator (creator_id),
    INDEX idx_task_status (status),
    INDEX idx_task_collaboration (collaboration_session_id),
    INDEX idx_task_workflow (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Material table
CREATE TABLE IF NOT EXISTS material (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    size BIGINT DEFAULT 0,
    type VARCHAR(128),
    storage_key VARCHAR(512),
    uploader VARCHAR(64),
    uploaded_at DATETIME,
    metadata JSON,
    kind VARCHAR(32),
    parse_status VARCHAR(32),
    parsed_content_id VARCHAR(64),
    source_channel VARCHAR(32),
    source_message_id VARCHAR(64),
    INDEX idx_material_workspace (workspace_id),
    INDEX idx_material_name (name),
    INDEX idx_material_uploader (uploader)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Parsed material content table
CREATE TABLE IF NOT EXISTS parsed_material_content (
    id VARCHAR(64) PRIMARY KEY,
    material_id VARCHAR(64) NOT NULL,
    text_content TEXT,
    structured_content JSON,
    metadata JSON,
    status VARCHAR(32),
    error_message TEXT,
    parsed_at DATETIME,
    INDEX idx_parsed_material (material_id),
    INDEX idx_parsed_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workflow table
CREATE TABLE IF NOT EXISTS workflow (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    nodes JSON,
    variables JSON,
    termination_config JSON,
    description TEXT,
    INDEX idx_workflow_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workflow state table
CREATE TABLE IF NOT EXISTS workflow_state (
    execution_id VARCHAR(64) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL,
    character_id VARCHAR(64),
    current_node_id VARCHAR(64),
    context JSON,
    results JSON,
    status VARCHAR(32) NOT NULL,
    current_step INT DEFAULT 0,
    loop_iteration INT DEFAULT 0,
    error_messages JSON,
    start_time DATETIME,
    end_time DATETIME,
    INDEX idx_state_workflow (workflow_id),
    INDEX idx_state_status (status),
    INDEX idx_state_character (character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Evaluation record table
CREATE TABLE IF NOT EXISTS evaluation_record (
    id VARCHAR(64) PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    plan_id VARCHAR(64),
    evaluation_type VARCHAR(32),
    dimensions JSON,
    findings JSON,
    unsafe_flags JSON,
    evidence_refs JSON,
    task_completion_score DOUBLE,
    efficiency_score DOUBLE,
    compliance_score DOUBLE,
    collaboration_score DOUBLE,
    satisfaction_score DOUBLE,
    overall_score DOUBLE,
    analysis TEXT,
    suggestions JSON,
    confidence DOUBLE DEFAULT 0.8,
    evidence JSON,
    timestamp DATETIME,
    evaluator VARCHAR(64),
    extensions JSON,
    INDEX idx_eval_target (target_type, target_id),
    INDEX idx_eval_task (task_id),
    INDEX idx_eval_plan (plan_id),
    INDEX idx_eval_timestamp (timestamp),
    INDEX idx_eval_overall (overall_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Modification log table
CREATE TABLE IF NOT EXISTS modification_log (
    id VARCHAR(64) PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    before_snapshot TEXT,
    after_snapshot TEXT,
    trigger_source VARCHAR(32),
    evaluation_id VARCHAR(64),
    reason VARCHAR(512),
    operator VARCHAR(64),
    timestamp DATETIME,
    extensions JSON,
    INDEX idx_mod_target (target_type, target_id),
    INDEX idx_mod_operator (operator),
    INDEX idx_mod_trigger (trigger_source),
    INDEX idx_mod_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optimization plan table
CREATE TABLE IF NOT EXISTS optimization_plan (
    id VARCHAR(64) PRIMARY KEY,
    observer_id VARCHAR(64) NOT NULL,
    evaluation_id VARCHAR(64),
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(255),
    summary TEXT,
    raw_content TEXT,
    items JSON,
    approver VARCHAR(64),
    approved_at DATETIME,
    approval_comment TEXT,
    executed_at DATETIME,
    completed_at DATETIME,
    execution_summary TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_plan_observer (observer_id),
    INDEX idx_plan_evaluation (evaluation_id),
    INDEX idx_plan_target (target_type, target_id),
    INDEX idx_plan_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optimization plan item table
CREATE TABLE IF NOT EXISTS optimization_plan_item (
    id VARCHAR(64) PRIMARY KEY,
    plan_id VARCHAR(64) NOT NULL,
    sequence INT NOT NULL DEFAULT 0,
    action_type VARCHAR(32),
    target_id VARCHAR(64),
    description TEXT,
    parameters JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    action_id VARCHAR(64),
    executed_at DATETIME,
    completed_at DATETIME,
    result TEXT,
    rolled_back_at DATETIME,
    rollback_result TEXT,
    created_at DATETIME,
    INDEX idx_item_plan (plan_id),
    INDEX idx_item_status (status),
    INDEX idx_item_action (action_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optimization action table
CREATE TABLE IF NOT EXISTS optimization_action (
    id VARCHAR(64) PRIMARY KEY,
    evaluation_id VARCHAR(64),
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    parameters JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    result TEXT,
    executed_at DATETIME,
    rolled_back_at DATETIME,
    rejection_reason TEXT,
    priority INT DEFAULT 0,
    created_at DATETIME,
    before_snapshot TEXT,
    after_snapshot TEXT,
    INDEX idx_action_evaluation (evaluation_id),
    INDEX idx_action_target (target_type, target_id),
    INDEX idx_action_status (status),
    INDEX idx_action_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Cron definition table
CREATE TABLE IF NOT EXISTS cron_definition (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by VARCHAR(64),
    cron_type VARCHAR(32),
    cron_expression VARCHAR(64),
    timezone VARCHAR(64) DEFAULT 'UTC',
    start_time BIGINT,
    end_time BIGINT,
    max_concurrent INT DEFAULT 1,
    job_type VARCHAR(32),
    job_handler VARCHAR(255) NOT NULL,
    job_data JSON,
    misfire_policy VARCHAR(32),
    timeout_ms INT,
    retry_count INT DEFAULT 0,
    retry_interval_ms INT,
    status VARCHAR(32) NOT NULL DEFAULT 'DISABLED',
    created_at BIGINT,
    updated_at BIGINT,
    version INT DEFAULT 1,
    INDEX idx_cron_status (status),
    INDEX idx_cron_type (cron_type),
    INDEX idx_cron_handler (job_handler)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Execution history table
CREATE TABLE IF NOT EXISTS execution_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL UNIQUE,
    cron_id VARCHAR(64),
    cron_name VARCHAR(255),
    trigger_time BIGINT,
    actual_fire_time BIGINT,
    complete_time BIGINT,
    duration_ms INT,
    status VARCHAR(32) NOT NULL,
    execute_node VARCHAR(64),
    execute_thread VARCHAR(64),
    result_data TEXT,
    error_message TEXT,
    stack_trace TEXT,
    retry_count INT DEFAULT 0,
    parent_execution_id VARCHAR(64),
    ext1 VARCHAR(128),
    ext2 VARCHAR(128),
    INDEX idx_history_cron (cron_id),
    INDEX idx_history_status (status),
    INDEX idx_history_trigger (trigger_time),
    INDEX idx_history_node (execute_node)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Channel config table
CREATE TABLE IF NOT EXISTS channel_config (
    id VARCHAR(64) PRIMARY KEY,
    channel_type VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    credentials JSON,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_config_channel (channel_type),
    INDEX idx_config_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Channel binding table
CREATE TABLE IF NOT EXISTS channel_binding (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    channel_name VARCHAR(32) NOT NULL,
    chat_id VARCHAR(64) NOT NULL,
    chat_type VARCHAR(16),
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSON,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_binding_workspace (workspace_id),
    INDEX idx_binding_channel (channel_name),
    INDEX idx_binding_chat (chat_id),
    UNIQUE KEY uk_channel_chat (channel_name, chat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Character duty table
CREATE TABLE IF NOT EXISTS character_duty (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    character_id VARCHAR(64) NOT NULL,
    duty_description TEXT,
    auto_generated BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_duty_workspace (workspace_id),
    INDEX idx_duty_character (character_id),
    UNIQUE KEY uk_workspace_character_duty (workspace_id, character_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Common sense folder table
CREATE TABLE IF NOT EXISTS common_sense_folder (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    parent_id VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_folder_workspace (workspace_id),
    INDEX idx_folder_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Common sense table
CREATE TABLE IF NOT EXISTS common_sense (
    id VARCHAR(64) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    folder_id VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(32),
    rule TEXT,
    severity VARCHAR(32),
    version INT DEFAULT 1,
    enabled BOOLEAN DEFAULT TRUE,
    prompt_template TEXT,
    prompt_variables JSON,
    content TEXT,
    cached_prompt TEXT,
    last_prompt_update_at DATETIME,
    prompt_update_source VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME,
    created_by VARCHAR(64),
    INDEX idx_cs_workspace (workspace_id),
    INDEX idx_cs_folder (folder_id),
    INDEX idx_cs_category (category),
    INDEX idx_cs_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Observer action log table
CREATE TABLE IF NOT EXISTS observer_action_log (
    id VARCHAR(64) PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    operator VARCHAR(64),
    details JSON,
    created_at DATETIME,
    INDEX idx_log_target (target_type, target_id),
    INDEX idx_log_action (action_type),
    INDEX idx_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V1: User & Config Tables
-- ============================================================================

-- User table
CREATE TABLE IF NOT EXISTS adeptify_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    phone VARCHAR(32) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(128),
    avatar VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    last_login_at DATETIME,
    last_login_ip VARCHAR(64),
    login_fail_count INT DEFAULT 0,
    lock_until DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User token table for refresh token management
CREATE TABLE IF NOT EXISTS user_token (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    device_info VARCHAR(255),
    ip_address VARCHAR(64),
    expires_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_refresh_token (refresh_token),
    INDEX idx_expires_at (expires_at),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES adeptify_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SMS verification code table
CREATE TABLE IF NOT EXISTS sms_code (
    id VARCHAR(64) PRIMARY KEY,
    phone VARCHAR(32) NOT NULL,
    code VARCHAR(8) NOT NULL,
    type VARCHAR(32) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_phone_code (phone, code, type),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Config store table
CREATE TABLE IF NOT EXISTS config_store (
    id VARCHAR(255) PRIMARY KEY,
    workspace VARCHAR(64),
    entity_type VARCHAR(64),
    entity_id VARCHAR(64),
    config_key VARCHAR(255) NOT NULL,
    config_value JSON,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_config_workspace (workspace),
    INDEX idx_config_entity (entity_type, entity_id),
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V2: Skill tables
-- ============================================================================

-- Skill metadata table
CREATE TABLE IF NOT EXISTS skill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL UNIQUE,
    category VARCHAR(32) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    tags VARCHAR(512),
    description TEXT,
    storage_path VARCHAR(1024) NOT NULL,
    skill_description TEXT NOT NULL,
    skill_content TEXT,
    requires_config TEXT,
    install_config TEXT,
    frontmatter_raw TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    creator_id BIGINT NOT NULL DEFAULT 0,
    creator_type VARCHAR(16) NOT NULL DEFAULT 'OFFICIAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_skill_name (name),
    INDEX idx_skill_category (category),
    INDEX idx_skill_enabled (enabled),
    INDEX idx_skill_visibility (visibility),
    INDEX idx_skill_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Skill binding table (workspace <-> skill)
CREATE TABLE IF NOT EXISTS skill_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_id BIGINT NOT NULL,
    workspace_id VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    binding_config JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_skill_workspace (skill_id, workspace_id),
    INDEX idx_binding_skill (skill_id),
    INDEX idx_binding_workspace (workspace_id),
    INDEX idx_binding_enabled (enabled),
    CONSTRAINT fk_binding_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V3: Skill bind table (different from skill_binding!)
-- ============================================================================

CREATE TABLE IF NOT EXISTS skill_bind (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_id BIGINT NOT NULL,
    bind_type VARCHAR(32) NOT NULL DEFAULT 'WORKSPACE',
    workspace_id VARCHAR(64),
    character_id VARCHAR(64),
    pinned_version INT,
    use_latest BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bind_skill (skill_id),
    INDEX idx_bind_workspace (workspace_id),
    INDEX idx_bind_character (character_id),
    INDEX idx_bind_type (bind_type),
    CONSTRAINT fk_bind_skill FOREIGN KEY (skill_id) REFERENCES skill(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V4: Registry tables (Character, Model, Observer, Tool)
-- ============================================================================

-- Character table
CREATE TABLE IF NOT EXISTS `character` (
    id VARCHAR(64) PRIMARY KEY,
    workspace_ids JSON,
    organization_ids JSON,
    name VARCHAR(255),
    version INT DEFAULT 0,
    description TEXT,
    avatar VARCHAR(512),
    source VARCHAR(64),
    allowed_tools JSON,
    traits JSON,
    trait_configs JSON,
    skills JSON,
    prompt_template TEXT,
    default_tools JSON,
    is_running BOOLEAN,
    deployed_count INT DEFAULT 0,
    mind_config JSON,
    extensions TEXT,
    status VARCHAR(32),
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_character_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Model instance table
CREATE TABLE IF NOT EXISTS model_instance (
    id VARCHAR(64) PRIMARY KEY,
    provider VARCHAR(32),
    model_name VARCHAR(128),
    endpoint VARCHAR(512),
    credentials JSON,
    default_params JSON,
    enabled BOOLEAN DEFAULT TRUE,
    description TEXT,
    priority INT DEFAULT 0,
    INDEX idx_model_provider (provider),
    INDEX idx_model_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Observer table
CREATE TABLE IF NOT EXISTS observer (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    workspace_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE',
    evaluation_mode VARCHAR(32),
    optimization_threshold DOUBLE,
    consecutive_low_score_threshold INT DEFAULT 3,
    common_sense_enabled BOOLEAN DEFAULT TRUE,
    auto_optimization_enabled BOOLEAN DEFAULT TRUE,
    periodic_evaluation_hours INT DEFAULT 24,
    properties JSON,
    planner_character_ids JSON,
    reviewer_character_ids JSON,
    supported_target_types JSON,
    manual_approval_required BOOLEAN DEFAULT TRUE,
    schedule_cron VARCHAR(64),
    plan_window_hours INT DEFAULT 24,
    max_plan_items INT DEFAULT 50,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_observer_workspace (workspace_id),
    INDEX idx_observer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tool table (stores metadata only, execution logic is in code)
CREATE TABLE IF NOT EXISTS tool (
    name VARCHAR(128) PRIMARY KEY,
    description TEXT,
    parameter_schema TEXT,
    enabled BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V5 + V6: Trait table (type column removed per V6)
-- ============================================================================

CREATE TABLE IF NOT EXISTS trait (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(512),
    content TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    used_by_count INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    INDEX idx_trait_category (category),
    INDEX idx_trait_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert initial trait data
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
-- V7 + V10: Approval tables (asset_collaborator dropped, approval_request updated)
-- ============================================================================

-- Approval request table (for publish, unpublish, add/remove collaborator approvals)
-- Note: asset_collaborator table was dropped in V10 (replaced by asset_member from V9)
CREATE TABLE IF NOT EXISTS approval_request (
    id VARCHAR(64) PRIMARY KEY,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    approval_type VARCHAR(32) NOT NULL,
    requester_id BIGINT NOT NULL,
    requester_name VARCHAR(255),
    approver_id BIGINT,
    approver_name VARCHAR(255),
    target_user_id BIGINT COMMENT 'For ADD_COLLABORATOR and REMOVE_COLLABORATOR approval types',
    reason TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME NOT NULL,
    processed_at DATETIME,
    processed_comment TEXT,
    INDEX idx_approval_resource (resource_type, resource_id),
    INDEX idx_approval_requester (requester_id),
    INDEX idx_approval_approver (approver_id),
    INDEX idx_approval_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- V9: RBAC tables (asset_member and permission_policy)
-- ============================================================================

-- asset_member: User's membership/role on an asset
CREATE TABLE IF NOT EXISTS asset_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_type VARCHAR(32) NOT NULL COMMENT 'WORKSPACE, CHARACTER, SKILL, etc.',
    resource_id VARCHAR(64) NOT NULL COMMENT 'ID of the resource',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role VARCHAR(32) NOT NULL COMMENT 'OWNER, ADMIN, COLLABORATOR, MEMBER',
    invited_by VARCHAR(64) COMMENT 'User ID who invited this member',
    invited_at DATETIME COMMENT 'When the invitation was sent',
    accepted_at DATETIME COMMENT 'When the invitation was accepted',
    accepted BOOLEAN DEFAULT FALSE COMMENT 'Whether invitation is accepted',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_asset_member (resource_type, resource_id, user_id),
    INDEX idx_asset_member_resource (resource_type, resource_id),
    INDEX idx_asset_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- permission_policy: Permissions granted by role+resource_type
CREATE TABLE IF NOT EXISTS permission_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_type VARCHAR(32) NOT NULL COMMENT 'Specific type or * for all',
    role VARCHAR(32) NOT NULL COMMENT 'OWNER, ADMIN, COLLABORATOR, MEMBER',
    permission JSON NOT NULL COMMENT 'Array of permissions',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy (role, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default permission policies
-- OWNER: 所有者，拥有全部权限
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WILDCARD', 'OWNER', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR", "TRANSFER"]');

-- ADMIN: 管理员，拥有编辑管理权限
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

-- COLLABORATOR: 协作者，仅能查看使用
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WILDCARD', 'COLLABORATOR', '["VIEW", "USE"]');

-- MEMBER: 成员，仅能查看（Workspace内资产）
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WORKSPACE', 'MEMBER', '["VIEW"]'),
('CHARACTER', 'MEMBER', '["VIEW"]'),
('OBSERVER', 'MEMBER', '["VIEW"]'),
('CONFIG', 'MEMBER', '["VIEW"]'),
('COMMONSENSE', 'MEMBER', '["VIEW"]');
