-- V7: Create collaborator and approval tables

-- Asset collaborator table (GitHub-style collaborator relationships)
CREATE TABLE asset_collaborator (
    id VARCHAR(64) PRIMARY KEY,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    collaborator_id BIGINT NOT NULL,
    invited_by VARCHAR(64) NOT NULL,
    invited_at DATETIME NOT NULL,
    accepted_at DATETIME,
    accepted BOOLEAN DEFAULT FALSE,
    INDEX idx_collaborator_resource (resource_type, resource_id),
    INDEX idx_collaborator_user (collaborator_id),
    UNIQUE KEY uk_collaborator_resource_user (resource_type, resource_id, collaborator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Approval request table (for publish, unpublish, add/remove collaborator approvals)
CREATE TABLE approval_request (
    id VARCHAR(64) PRIMARY KEY,
    resource_type VARCHAR(32) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    approval_type VARCHAR(32) NOT NULL,
    requester_id BIGINT NOT NULL,
    requester_name VARCHAR(255),
    approver_id BIGINT,
    approver_name VARCHAR(255),
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
