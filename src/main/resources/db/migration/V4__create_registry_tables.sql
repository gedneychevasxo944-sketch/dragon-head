-- V4: Create registry tables for Character, Model, Observer, Tool persistence

-- Character table
CREATE TABLE character (
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
    INDEX idx_character_workspace (workspace_ids(255)),
    INDEX idx_character_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Model instance table
CREATE TABLE model_instance (
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
CREATE TABLE observer (
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
CREATE TABLE tool (
    name VARCHAR(128) PRIMARY KEY,
    description TEXT,
    parameter_schema TEXT,
    enabled BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;