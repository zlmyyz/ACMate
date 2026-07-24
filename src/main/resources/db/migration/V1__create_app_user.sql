CREATE TABLE IF NOT EXISTS app_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username        VARCHAR(32)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    nickname        VARCHAR(32)     NOT NULL,
    email           VARCHAR(128)    NULL,
    avatar_url      VARCHAR(512)    NULL,
    is_admin        TINYINT         NOT NULL DEFAULT 0 COMMENT '0普通用户,1管理员',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0禁用,1正常',
    last_login_time DATETIME        NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status_create_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
