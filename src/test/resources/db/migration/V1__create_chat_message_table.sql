-- Chat message table for MySQL storage
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
    INDEX idx_workspace_id (workspace_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_session_id (session_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;