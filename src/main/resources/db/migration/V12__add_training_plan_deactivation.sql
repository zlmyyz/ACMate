ALTER TABLE training_plan
    ADD COLUMN deactivation_source VARCHAR(20) NULL COMMENT 'CREATOR,ADMIN' AFTER is_active,
    ADD COLUMN deactivation_reason VARCHAR(500) NULL AFTER deactivation_source,
    ADD COLUMN deactivated_by BIGINT UNSIGNED NULL AFTER deactivation_reason,
    ADD COLUMN deactivation_time DATETIME NULL AFTER deactivated_by;
