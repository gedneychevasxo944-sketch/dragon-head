-- V6: Remove type column from trait table
-- Trait 不再区分 personality/config 类型，统一使用 category 分类

ALTER TABLE trait DROP COLUMN type;