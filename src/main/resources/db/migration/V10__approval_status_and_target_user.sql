-- V10: Update approval_request table + Drop deprecated tables
-- Adds target_user_id column and ensures status column is properly typed

-- Add target_user_id column for ADD_COLLABORATOR and REMOVE_COLLABORATOR approval types
ALTER TABLE approval_request ADD COLUMN target_user_id BIGINT AFTER approver_name;

-- Ensure status column can store enum values (it should already be VARCHAR(32) from V7)
-- This ensures compatibility with the new ApprovalStatus enum
ALTER TABLE approval_request MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

-- Drop deprecated asset_collaborator table (data migrated to asset_member in V9)
DROP TABLE IF EXISTS asset_collaborator;
