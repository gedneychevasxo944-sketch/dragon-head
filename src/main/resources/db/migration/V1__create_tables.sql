-- V1: Create all business tables for MySQL storage
-- Generated from Entity classes

-- Chat message table
CREATE TABLE chat_message (
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
CREATE TABLE chat_session (
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
CREATE TABLE workspace (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INACTIVE',
    properties JSON,
    personality JSON,
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_workspace_owner (owner),
    INDEX idx_workspace_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workspace member table
CREATE TABLE workspace_member (
    id VARCHAR(128) PRIMARY KEY,
    workspace_id VARCHAR(64) NOT NULL,
    character_id VARCHAR(64) NOT NULL,
    role VARCHAR(64),
    layer VARCHAR(32) DEFAULT 'NORMAL',
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
CREATE TABLE task (
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
CREATE TABLE material (
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
CREATE TABLE parsed_material_content (
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
CREATE TABLE workflow (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    nodes JSON,
    variables JSON,
    termination_config JSON,
    description TEXT,
    INDEX idx_workflow_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workflow state table
CREATE TABLE workflow_state (
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
CREATE TABLE evaluation_record (
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
CREATE TABLE modification_log (
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
CREATE TABLE optimization_plan (
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
CREATE TABLE optimization_plan_item (
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
CREATE TABLE optimization_action (
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
CREATE TABLE cron_definition (
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
CREATE TABLE execution_history (
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
CREATE TABLE channel_config (
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
CREATE TABLE channel_binding (
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
CREATE TABLE character_duty (
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
CREATE TABLE common_sense_folder (
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
CREATE TABLE common_sense (
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
CREATE TABLE observer_action_log (
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

-- ==================== V2: User & Config Tables ====================

-- User table
CREATE TABLE adeptify_user (
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
CREATE TABLE user_token (
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
CREATE TABLE sms_code (
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
CREATE TABLE config_store (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add visibility and permission columns to existing tables
ALTER TABLE workspace ADD COLUMN visibility VARCHAR(32) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC or PRIVATE';
ALTER TABLE workspace_member ADD COLUMN permission VARCHAR(32) NOT NULL DEFAULT 'VIEW' COMMENT 'VIEW, USE, EDIT, ADMIN, OWNER';