-- V100: Add tags column to skills table for skill classification and scenario tagging

ALTER TABLE skills ADD COLUMN tags JSON COMMENT '标签列表，用于技能分类/场景归纳，如 ["数据分析","API","工具类"]';
