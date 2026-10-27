CREATE TABLE user_login_identity (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    login_name VARCHAR(255) NULL,
    display_name VARCHAR(255) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_login_identity (provider, identifier),
    UNIQUE KEY uk_user_login_provider (user_id, provider),
    CONSTRAINT fk_login_identity_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_login_identity_provider CHECK (provider IN ('EMAIL', 'GITHUB'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO user_login_identity(user_id,provider,identifier)
SELECT id,'EMAIL',LOWER(TRIM(email)) FROM app_user
WHERE email IS NOT NULL AND TRIM(email)<>'' AND merged_into_user_id IS NULL;

INSERT INTO user_login_identity(user_id,provider,identifier,login_name,display_name)
SELECT user_id,'GITHUB',CAST(github_id AS CHAR),github_login,github_name FROM user_github;

-- Keep the original data for rollback/audit. Runtime reads use the unified table.
RENAME TABLE user_github TO user_github_legacy;
CREATE VIEW user_github AS
SELECT user_id,CAST(identifier AS UNSIGNED) AS github_id,login_name AS github_login,
       display_name AS github_name,create_time,update_time
FROM user_login_identity WHERE provider='GITHUB';
