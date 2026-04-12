-- Create memory_chunk table for chunk metadata management
CREATE TABLE IF NOT EXISTS memory_chunk (
                                            id              VARCHAR(64)  NOT NULL PRIMARY KEY,
    source_id       VARCHAR(64)  NOT NULL COMMENT 'Associated data source ID',
    title           VARCHAR(255)          COMMENT 'Chunk title',
    content         TEXT                  COMMENT 'Chunk content',
    summary         TEXT                  COMMENT 'Chunk summary',
    tags            TEXT                  COMMENT 'Tag list in JSON format, e.g. ["tag1","tag2"]',
    indexed_status  VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT 'Index status: pending/indexed/failed',
    relations       TEXT                  COMMENT 'Related chunk IDs in JSON format',
    file_path       VARCHAR(512)          COMMENT 'File path',
    file_type       VARCHAR(32)           COMMENT 'File type: markdown/json/text/other',
    total_size      BIGINT                COMMENT 'File size in bytes',
    sync_status     VARCHAR(32)           COMMENT 'Sync status: synced/syncing/pending/failed/disabled',
    health_status   VARCHAR(32)           COMMENT 'Health status: healthy/warning/error/unknown',
    last_sync_at    DATETIME              COMMENT 'Last sync time',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_chunk_source (source_id),
    INDEX idx_chunk_status (indexed_status),
    INDEX idx_chunk_sync_status (sync_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Memory chunk metadata';