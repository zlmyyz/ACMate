ALTER TABLE app_user
    ADD COLUMN bio VARCHAR(500) NULL COMMENT '个人简介' AFTER avatar_url;
