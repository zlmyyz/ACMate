CREATE TABLE IF NOT EXISTS problem (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    platform             VARCHAR(20)     NOT NULL COMMENT 'CUSTOM,CODEFORCES,NOWCODER,OTHER',
    external_problem_key VARCHAR(64)     NULL,
    title                VARCHAR(255)    NOT NULL,
    source_url           VARCHAR(1024)   NULL,
    difficulty           VARCHAR(32)     NULL,
    tags                 VARCHAR(255)    NULL,
    content_md           MEDIUMTEXT      NULL,
    creator_user_id      BIGINT UNSIGNED NOT NULL,
    status               TINYINT         NOT NULL DEFAULT 1 COMMENT '0禁用,1正常',
    create_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_problem (platform, external_problem_key),
    KEY idx_creator_user_id (creator_user_id),
    KEY idx_platform_status (platform, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_plan (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title           VARCHAR(128)    NOT NULL,
    description     TEXT            NULL,
    start_time      DATETIME        NULL,
    end_time        DATETIME        NULL,
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '0草稿,1已发布,2已结束',
    creator_user_id BIGINT UNSIGNED NOT NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time, end_time),
    KEY idx_tp_creator_user_id (creator_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_plan_problem (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    plan_id       BIGINT UNSIGNED NOT NULL,
    problem_id    BIGINT UNSIGNED NOT NULL,
    sort_order    INT             NOT NULL DEFAULT 0,
    required_flag TINYINT         NOT NULL DEFAULT 1 COMMENT '0选做,1必做',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_problem (plan_id, problem_id),
    KEY idx_problem_id (problem_id),
    KEY idx_plan_sort (plan_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_problem_status (
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id                BIGINT UNSIGNED NOT NULL,
    problem_id             BIGINT UNSIGNED NOT NULL,
    status                 TINYINT         NOT NULL DEFAULT 0 COMMENT '0未开始,1尝试过,2已通过',
    attempt_count          INT             NOT NULL DEFAULT 0,
    first_submit_time      DATETIME        NULL,
    first_ac_time          DATETIME        NULL,
    last_submit_time       DATETIME        NULL,
    solve_source           VARCHAR(20)     NULL COMMENT 'MANUAL,CODEFORCES,NOWCODER',
    accepted_submission_id BIGINT UNSIGNED NULL,
    create_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_problem (user_id, problem_id),
    KEY idx_user_status (user_id, status),
    KEY idx_first_ac_time (first_ac_time),
    KEY idx_problem_status (problem_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
