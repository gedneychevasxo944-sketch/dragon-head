-- Add target_name column to observer_action_log table for audit log feature
ALTER TABLE observer_action_log ADD COLUMN target_name VARCHAR(255) DEFAULT NULL AFTER target_id;
