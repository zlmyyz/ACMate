ALTER TABLE training_plan
    DROP COLUMN status,
    ADD COLUMN plan_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL' COMMENT 'PERSONAL,PUBLIC' AFTER description,
    ADD COLUMN is_active TINYINT NOT NULL DEFAULT 1 COMMENT '0停用,1正常' AFTER plan_type;

CREATE TABLE IF NOT EXISTS training_plan_member (
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_id   BIGINT UNSIGNED NOT NULL,
    user_id   BIGINT UNSIGNED NOT NULL,
    join_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_user (plan_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
