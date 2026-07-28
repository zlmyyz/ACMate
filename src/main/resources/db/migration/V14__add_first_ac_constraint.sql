CREATE TABLE IF NOT EXISTS oj_first_ac (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id               BIGINT UNSIGNED NOT NULL,
    platform              VARCHAR(20)     NOT NULL,
    external_problem_key  VARCHAR(64)     NOT NULL,
    submission_id         BIGINT UNSIGNED NOT NULL,
    create_time           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform_problem (user_id, platform, external_problem_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
