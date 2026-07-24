CREATE TABLE IF NOT EXISTS oj_account (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    platform          VARCHAR(20)     NOT NULL COMMENT 'CODEFORCES,NOWCODER',
    external_user_id  VARCHAR(128)    NOT NULL,
    display_name      VARCHAR(128)    NULL,
    verify_status     TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审核,1已验证,2已拒绝',
    sync_enabled      TINYINT         NOT NULL DEFAULT 1 COMMENT '0禁用同步,1启用同步',
    last_sync_cursor  VARCHAR(128)    NULL,
    last_sync_time    DATETIME        NULL,
    last_sync_success TINYINT         NULL COMMENT '0失败,1成功',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform (user_id, platform),
    UNIQUE KEY uk_platform_external_user (platform, external_user_id),
    KEY idx_sync_account (platform, verify_status, sync_enabled, last_sync_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS oj_submission (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    oj_account_id         BIGINT UNSIGNED NOT NULL,
    user_id               BIGINT UNSIGNED NOT NULL,
    platform              VARCHAR(20)     NOT NULL,
    remote_submission_id  VARCHAR(64)     NOT NULL,
    problem_id            BIGINT UNSIGNED NULL,
    external_problem_key  VARCHAR(64)     NOT NULL,
    verdict               VARCHAR(32)     NOT NULL,
    language              VARCHAR(64)     NULL,
    submitted_time        DATETIME        NOT NULL,
    is_first_ac           TINYINT         NOT NULL DEFAULT 0,
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_submission (platform, remote_submission_id),
    KEY idx_account_time (oj_account_id, submitted_time),
    KEY idx_user_verdict_time (user_id, verdict, submitted_time),
    KEY idx_oj_submission_user_problem (user_id, problem_id),
    KEY idx_external_problem (platform, external_problem_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sync_task_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    oj_account_id   BIGINT UNSIGNED NOT NULL,
    platform        VARCHAR(20)     NOT NULL,
    trigger_type    VARCHAR(20)     NOT NULL COMMENT 'SCHEDULED,MANUAL',
    task_status     VARCHAR(20)     NOT NULL COMMENT 'RUNNING,SUCCESS,FAILED',
    cursor_before   VARCHAR(128)    NULL,
    cursor_after    VARCHAR(128)    NULL,
    fetched_count   INT             NOT NULL DEFAULT 0,
    inserted_count  INT             NOT NULL DEFAULT 0,
    first_ac_count  INT             NOT NULL DEFAULT 0,
    error_message   VARCHAR(1000)   NULL,
    start_time      DATETIME        NOT NULL,
    end_time        DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_account_start_time (oj_account_id, start_time),
    KEY idx_status_start_time (task_status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
