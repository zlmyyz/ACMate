ALTER TABLE problem
    ADD COLUMN deactivation_source VARCHAR(20) NULL COMMENT 'CREATOR,ADMIN' AFTER status,
    ADD COLUMN deactivation_reason VARCHAR(500) NULL COMMENT '停用原因' AFTER deactivation_source,
    ADD COLUMN deactivated_by BIGINT UNSIGNED NULL COMMENT '操作人用户ID' AFTER deactivation_reason,
    ADD COLUMN deactivation_time DATETIME NULL COMMENT '停用时间' AFTER deactivated_by;
