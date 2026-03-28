-- V2: Create user management tables
-- User authentication and authorization

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

-- Add visibility and permission columns to existing tables
ALTER TABLE workspace ADD COLUMN visibility VARCHAR(32) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC or PRIVATE';
ALTER TABLE workspace_member ADD COLUMN permission VARCHAR(32) NOT NULL DEFAULT 'VIEW' COMMENT 'VIEW, USE, EDIT, ADMIN, OWNER';
