-- V2: Add config store table for MySQL ConfigStore

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
