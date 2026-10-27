ALTER TABLE problem MODIFY creator_user_id BIGINT UNSIGNED NULL;

CREATE TABLE problem_cache_revision (id TINYINT NOT NULL PRIMARY KEY, revision BIGINT UNSIGNED NOT NULL DEFAULT 0) ENGINE=InnoDB;
INSERT INTO problem_cache_revision(id,revision) VALUES(1,0);
CREATE TRIGGER problem_cache_insert AFTER INSERT ON problem FOR EACH ROW UPDATE problem_cache_revision SET revision=revision+1 WHERE id=1;
CREATE TRIGGER problem_cache_update AFTER UPDATE ON problem FOR EACH ROW UPDATE problem_cache_revision SET revision=revision+1 WHERE id=1;
CREATE TRIGGER problem_cache_delete AFTER DELETE ON problem FOR EACH ROW UPDATE problem_cache_revision SET revision=revision+1 WHERE id=1;

CREATE TABLE problem_change_request (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  problem_id BIGINT UNSIGNED NOT NULL,
  author_user_id BIGINT UNSIGNED NOT NULL,
  base_update_time DATETIME NOT NULL,
  base_fingerprint CHAR(64) NOT NULL,
  original_content MEDIUMTEXT NOT NULL,
  proposed_content MEDIUMTEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  reviewer_user_id BIGINT UNSIGNED NULL,
  review_time DATETIME NULL,
  review_note VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_problem_change_history(problem_id, id),
  KEY idx_problem_change_pending(status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE nowcoder_account_owner (
  uid VARCHAR(128) NOT NULL PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT IGNORE INTO nowcoder_account_owner(uid,user_id)
SELECT external_user_id,user_id FROM oj_account WHERE platform='NOWCODER';
