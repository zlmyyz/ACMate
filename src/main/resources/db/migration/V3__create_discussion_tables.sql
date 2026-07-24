CREATE TABLE IF NOT EXISTS post (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_user_id       BIGINT UNSIGNED NOT NULL,
    problem_id           BIGINT UNSIGNED NULL,
    training_plan_id     BIGINT UNSIGNED NULL,
    post_type            VARCHAR(20)     NOT NULL COMMENT 'DISCUSSION,SOLUTION,HELP,NOTICE',
    title                VARCHAR(255)    NOT NULL,
    content_md           MEDIUMTEXT      NOT NULL,
    status               TINYINT         NOT NULL DEFAULT 1 COMMENT '0删除,1正常',
    is_pinned            TINYINT         NOT NULL DEFAULT 0 COMMENT '0不置顶,1置顶',
    accepted_comment_id  BIGINT UNSIGNED NULL COMMENT '采纳的评论ID,仅用于HELP',
    view_count           INT UNSIGNED    NOT NULL DEFAULT 0,
    like_count           INT UNSIGNED    NOT NULL DEFAULT 0,
    comment_count        INT UNSIGNED    NOT NULL DEFAULT 0,
    create_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_author_user_id (author_user_id),
    KEY idx_problem_id (problem_id),
    KEY idx_training_plan_id (training_plan_id),
    KEY idx_type_status_time (post_type, status, create_time),
    KEY idx_pinned_time (is_pinned, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS post_comment (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id           BIGINT UNSIGNED NOT NULL,
    user_id           BIGINT UNSIGNED NOT NULL,
    parent_id         BIGINT UNSIGNED NULL COMMENT '所属一级评论ID,NULL表示一级评论',
    reply_to_user_id  BIGINT UNSIGNED NULL COMMENT '回复目标用户ID',
    content           TEXT            NOT NULL,
    status            TINYINT         NOT NULL DEFAULT 1 COMMENT '0删除,1正常',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_post_parent_time (post_id, parent_id, create_time),
    KEY idx_comment_user_id (user_id),
    KEY idx_reply_to_user_id (reply_to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS post_like (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    post_id     BIGINT UNSIGNED NOT NULL,
    user_id     BIGINT UNSIGNED NOT NULL,
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_like_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
