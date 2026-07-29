package com.itnoduck.acmate.auditlog;

public final class AuditLogConstants {

    private AuditLogConstants() {}

    // action types
    public static final String USER_DEACTIVATED = "USER_DEACTIVATED";
    public static final String USER_RESTORED = "USER_RESTORED";
    public static final String ADMIN_GRANTED = "ADMIN_GRANTED";
    public static final String ADMIN_REVOKED = "ADMIN_REVOKED";

    public static final String POST_ADMIN_DEACTIVATED = "POST_ADMIN_DEACTIVATED";
    public static final String POST_RESTORED = "POST_RESTORED";
    public static final String COMMENT_ADMIN_DEACTIVATED = "COMMENT_ADMIN_DEACTIVATED";
    public static final String COMMENT_RESTORED = "COMMENT_RESTORED";

    public static final String TRAINING_ADMIN_DEACTIVATED = "TRAINING_ADMIN_DEACTIVATED";
    public static final String TRAINING_RESTORED = "TRAINING_RESTORED";

    public static final String PROBLEM_ADMIN_DEACTIVATED = "PROBLEM_ADMIN_DEACTIVATED";
    public static final String PROBLEM_RESTORED = "PROBLEM_RESTORED";

    public static final String OJ_ACCOUNT_VERIFIED = "OJ_ACCOUNT_VERIFIED";
    public static final String OJ_ACCOUNT_REJECTED = "OJ_ACCOUNT_REJECTED";

    // target types
    public static final String TARGET_USER = "USER";
    public static final String TARGET_POST = "POST";
    public static final String TARGET_COMMENT = "COMMENT";
    public static final String TARGET_TRAINING_PLAN = "TRAINING_PLAN";
    public static final String TARGET_PROBLEM = "PROBLEM";
    public static final String TARGET_OJ_ACCOUNT = "OJ_ACCOUNT";

    public static final java.util.Set<String> VALID_ACTION_TYPES = java.util.Set.of(
        USER_DEACTIVATED, USER_RESTORED, ADMIN_GRANTED, ADMIN_REVOKED,
        POST_ADMIN_DEACTIVATED, POST_RESTORED,
        COMMENT_ADMIN_DEACTIVATED, COMMENT_RESTORED,
        TRAINING_ADMIN_DEACTIVATED, TRAINING_RESTORED,
        PROBLEM_ADMIN_DEACTIVATED, PROBLEM_RESTORED,
        OJ_ACCOUNT_VERIFIED, OJ_ACCOUNT_REJECTED
    );

    public static final java.util.Set<String> VALID_TARGET_TYPES = java.util.Set.of(
        TARGET_USER, TARGET_POST, TARGET_COMMENT,
        TARGET_TRAINING_PLAN, TARGET_PROBLEM, TARGET_OJ_ACCOUNT
    );
}
