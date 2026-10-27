ALTER TABLE app_user
    ADD COLUMN location VARCHAR(128) NULL,
    ADD COLUMN gender VARCHAR(16) NULL,
    ADD COLUMN last_login_ip VARCHAR(64) NULL,
    ADD COLUMN last_login_region VARCHAR(128) NULL;

CREATE TABLE user_follow (
    follower_id BIGINT UNSIGNED NOT NULL,
    followed_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, followed_id),
    KEY idx_followed_created (followed_id, create_time),
    CONSTRAINT chk_follow_not_self CHECK (follower_id <> followed_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_favorite (
    user_id BIGINT UNSIGNED NOT NULL,
    post_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    KEY idx_favorite_post_time (post_id, create_time),
    KEY idx_favorite_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_daily_views (
    post_id BIGINT UNSIGNED NOT NULL,
    stat_date DATE NOT NULL,
    view_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (post_id, stat_date),
    KEY idx_daily_views_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
