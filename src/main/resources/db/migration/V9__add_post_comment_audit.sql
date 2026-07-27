ALTER TABLE post
    ADD COLUMN deactivation_source VARCHAR(10) NULL COMMENT 'CREATOR/ADMIN',
    ADD COLUMN deactivation_reason VARCHAR(500) NULL,
    ADD COLUMN deactivated_by BIGINT UNSIGNED NULL,
    ADD COLUMN deactivation_time DATETIME NULL;

ALTER TABLE post_comment
    ADD COLUMN deactivation_source VARCHAR(10) NULL COMMENT 'CREATOR/ADMIN',
    ADD COLUMN deactivation_reason VARCHAR(500) NULL,
    ADD COLUMN deactivated_by BIGINT UNSIGNED NULL,
    ADD COLUMN deactivation_time DATETIME NULL;
