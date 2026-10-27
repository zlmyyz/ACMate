ALTER TABLE user_github
    ADD COLUMN github_login VARCHAR(64) NULL,
    ADD COLUMN github_name VARCHAR(255) NULL;

ALTER TABLE app_user
    ADD COLUMN merged_into_user_id BIGINT UNSIGNED NULL,
    ADD INDEX idx_user_merged_into (merged_into_user_id);

CREATE TABLE account_merge_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    target_user_id BIGINT UNSIGNED NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
