-- V2: Add Skill tables
-- Skill metadata table
CREATE TABLE skill (
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
CREATE TABLE skill_binding (
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