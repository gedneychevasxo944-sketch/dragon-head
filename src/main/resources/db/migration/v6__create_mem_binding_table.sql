-- Create mem_binding table for memory binding relationships
CREATE TABLE IF NOT EXISTS mem_binding (
                                           id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    file_id         VARCHAR(64)  NOT NULL COMMENT 'Associated file ID',
    chunk_id        VARCHAR(64)  NOT NULL COMMENT 'Associated chunk ID',
    target_type     VARCHAR(32)  NOT NULL COMMENT 'Binding target type: character/workspace',
    target_id       VARCHAR(64)  NOT NULL COMMENT 'Binding target ID',
    target_name     VARCHAR(255)          COMMENT 'Binding target name',
    mount_type      VARCHAR(32)           COMMENT 'Mount type: full/selective/rule',
    snapshot_file_name VARCHAR(255)       COMMENT 'Generated snapshot file path, e.g. mem/xxx.md',
    source_id       VARCHAR(64)           COMMENT 'Source document ID',
    memory_id       VARCHAR(64)           COMMENT 'Corresponding memory entry ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_binding_target (target_type, target_id),
    INDEX idx_binding_file (file_id),
    INDEX idx_binding_chunk (chunk_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Memory binding relationships';