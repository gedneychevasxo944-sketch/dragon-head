-- Skill 操作日志表
CREATE TABLE skill_action_log (
    id              VARCHAR(64) PRIMARY KEY COMMENT '主键 UUID',
    skill_id        VARCHAR(64) NOT NULL COMMENT '技能 UUID',
    skill_name      VARCHAR(100) COMMENT '技能名称（冗余，便于展示）',
    action_type     VARCHAR(50) NOT NULL COMMENT '动作类型',
    operator_id     BIGINT UNSIGNED COMMENT '操作人 ID',
    operator_name   VARCHAR(100) COMMENT '操作人名称（冗余）',
    version         INT UNSIGNED COMMENT '涉及版本号',
    detail          JSON COMMENT '操作详情，结构因 action_type 而异',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_skill_action (skill_id, action_type),
    INDEX idx_skill_time (skill_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 操作日志表';