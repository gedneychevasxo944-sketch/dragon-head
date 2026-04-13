-- ============================================================================
-- Add MBTI Column to Character Table
-- Version: V6
-- Description: Add mbti column to support MBTI personality type for Characters
-- ============================================================================

USE adeptify;

ALTER TABLE `character` ADD COLUMN mbti VARCHAR(4) DEFAULT NULL;