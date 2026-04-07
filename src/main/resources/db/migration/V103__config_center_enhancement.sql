-- ============================================================================
-- V103: Config Center Enhancement
-- 1. config_store 表添加元数据字段
-- 2. 删除 config_definitions 表
-- ============================================================================

-- 1. config_store 表添加元数据字段
ALTER TABLE config_store
  ADD COLUMN `name` VARCHAR(128) DEFAULT NULL COMMENT '配置项名称',
  ADD COLUMN `description` VARCHAR(500) DEFAULT NULL COMMENT '配置项描述',
  ADD COLUMN `validation_rules` JSON DEFAULT NULL COMMENT '校验规则JSON数组',
  ADD COLUMN `options` JSON DEFAULT NULL COMMENT '枚举选项JSON数组',
  ADD COLUMN `modified_by` VARCHAR(100) DEFAULT NULL;

-- 添加索引
CREATE INDEX idx_config_key_name ON config_store(config_key, name);

-- 2. 删除 config_definitions 表（已废弃，元数据统一用 config_store 的 GLOBAL 行存储）
DROP TABLE IF EXISTS config_definitions;
