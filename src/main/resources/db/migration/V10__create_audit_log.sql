CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_id     BIGINT UNSIGNED NOT NULL,
    action          VARCHAR(64)     NOT NULL,
    resource_type   VARCHAR(32)     NOT NULL,
    resource_id     BIGINT UNSIGNED NULL,
    reason          VARCHAR(500)    NULL,
    before_state    VARCHAR(2000)   NULL,
    after_state     VARCHAR(2000)   NULL,
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_operator_time (operator_id, create_time DESC),
    KEY idx_resource (resource_type, resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
