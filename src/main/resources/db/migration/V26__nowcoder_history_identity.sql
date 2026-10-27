ALTER TABLE oj_submission ADD COLUMN nowcoder_uid VARCHAR(128) NULL;
CREATE INDEX idx_nowcoder_uid_time ON oj_submission(user_id,platform,nowcoder_uid,submitted_time);
UPDATE oj_submission s JOIN oj_account a ON a.id=s.oj_account_id AND a.platform='NOWCODER' SET s.nowcoder_uid=a.external_user_id WHERE s.platform='NOWCODER' AND s.nowcoder_uid IS NULL;
UPDATE oj_submission s JOIN nowcoder_sync_job j ON j.account_id=s.oj_account_id SET s.nowcoder_uid=j.uid WHERE s.platform='NOWCODER' AND s.nowcoder_uid IS NULL;
