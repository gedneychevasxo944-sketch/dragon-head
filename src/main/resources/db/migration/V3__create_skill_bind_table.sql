-- V3: Add skill_bind table
CREATE TABLE skill_bind (
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