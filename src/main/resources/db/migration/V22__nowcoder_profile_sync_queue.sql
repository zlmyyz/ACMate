ALTER TABLE oj_submission ADD COLUMN external_problem_title VARCHAR(255) NULL;

CREATE TABLE nowcoder_sync_job (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    uid VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    next_page INT NOT NULL DEFAULT 1,
    total_pages INT NOT NULL DEFAULT 0,
    fetched_count INT NOT NULL DEFAULT 0,
    inserted_count INT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    next_run_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_token VARCHAR(36) NULL,
    lease_until DATETIME NULL,
    error_message VARCHAR(255) NULL,
    request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_nowcoder_sync_account(account_id),
    KEY idx_nowcoder_sync_queue(status,next_run_time,lease_until)
);

CREATE TABLE nowcoder_problem_import_job (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    external_problem_key VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    problem_id BIGINT UNSIGNED NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_run_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_token VARCHAR(36) NULL,
    lease_until DATETIME NULL,
    error_message VARCHAR(255) NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_nowcoder_import_problem(external_problem_key),
    KEY idx_nowcoder_import_queue(status,next_run_time,lease_until)
);
