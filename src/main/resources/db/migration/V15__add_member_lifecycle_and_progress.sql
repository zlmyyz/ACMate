ALTER TABLE training_plan_member
    ADD COLUMN status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1=ACTIVE,0=REMOVED' AFTER join_time,
    ADD COLUMN remove_time DATETIME        NULL                                  AFTER status,
    ADD COLUMN removed_by  BIGINT UNSIGNED NULL                                  AFTER remove_time;

CREATE INDEX idx_member_active ON training_plan_member (plan_id, status, user_id);

ALTER TABLE user_problem_status
    ADD COLUMN performance_note VARCHAR(500) NULL COMMENT '用户本人填写,最长500字' AFTER accepted_submission_id;

INSERT INTO user_problem_status (user_id, problem_id, status, first_ac_time, solve_source, accepted_submission_id)
SELECT fa.user_id, p.id, 2, o.submitted_time, fa.platform, fa.submission_id
FROM oj_first_ac fa
JOIN problem p ON p.platform = fa.platform AND p.external_problem_key = fa.external_problem_key
JOIN oj_submission o ON o.id = fa.submission_id
ON DUPLICATE KEY UPDATE
    status          = 2,
    accepted_submission_id = COALESCE(user_problem_status.accepted_submission_id, VALUES(accepted_submission_id)),
    solve_source    = VALUES(solve_source),
    first_ac_time   = COALESCE(user_problem_status.first_ac_time, VALUES(first_ac_time));
