CREATE TABLE IF NOT EXISTS notification (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    type            VARCHAR(32)     NOT NULL COMMENT 'PLAN_UPDATE,PLAN_START,PLAN_REMOVE,POST_REPLY,CONTENT_DEACTIVATE,OJ_VERIFY,OJ_SYNC_FAIL',
    title           VARCHAR(255)    NOT NULL,
    content         VARCHAR(1000)   NULL,
    resource_type   VARCHAR(32)     NULL COMMENT 'problem,post,plan,oj_account',
    resource_id     BIGINT UNSIGNED NULL,
    is_read         TINYINT         NOT NULL DEFAULT 0,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_read_time (user_id, is_read, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
