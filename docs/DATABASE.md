# DATABASE

## 数据库概览

数据库名称：acmate

共 11 张表：

| 表名 | 说明 | 迁移文件 |
| --- | --- | --- |
| app_user | 用户表 | V1 |
| problem | 题目表 | V2 |
| training_plan | 训练计划表 | V2 |
| training_plan_problem | 训练计划-题目关联表 | V2 |
| user_problem_status | 用户题目完成状态表 | V2 |
| post | 帖子表 | V3 |
| post_comment | 评论表 | V3 |
| post_like | 点赞表 | V3 |
| oj_account | 外部 OJ 账号绑定表 | V4 |
| oj_submission | 外部 OJ 提交记录表 | V4 |
| sync_task_log | 同步任务日志表 | V4 |

## 字段说明

### app_user

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| username | VARCHAR(32) | 用户名，唯一 |
| password_hash | VARCHAR(255) | 密码哈希，不可存明文 |
| nickname | VARCHAR(32) | 昵称 |
| email | VARCHAR(128) | 邮箱，唯一 |
| avatar_url | VARCHAR(512) | 头像地址 |
| is_admin | TINYINT | 0普通用户，1管理员 |
| status | TINYINT | 0禁用，1正常 |
| last_login_time | DATETIME | 最后登录时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间（自动更新） |

### problem

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| platform | VARCHAR(20) | CUSTOM / CODEFORCES / NOWCODER / OTHER |
| external_problem_key | VARCHAR(64) | 外部平台题目标识 |
| title | VARCHAR(255) | 题目标题 |
| source_url | VARCHAR(1024) | 题目原文链接 |
| difficulty | VARCHAR(32) | 难度 |
| tags | VARCHAR(255) | 标签 |
| content_md | MEDIUMTEXT | Markdown 题目内容 |
| creator_user_id | BIGINT UNSIGNED | 创建者 ID |
| status | TINYINT | 0禁用，1正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### training_plan

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| title | VARCHAR(128) | 计划标题 |
| description | TEXT | 计划描述 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| status | TINYINT | 0草稿，1已发布，2已结束 |
| creator_user_id | BIGINT UNSIGNED | 创建者 ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### training_plan_problem

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| plan_id | BIGINT UNSIGNED | 训练计划 ID |
| problem_id | BIGINT UNSIGNED | 题目 ID |
| sort_order | INT | 排序序号 |
| required_flag | TINYINT | 0选做，1必做 |
| create_time | DATETIME | 创建时间 |

### user_problem_status

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| user_id | BIGINT UNSIGNED | 用户 ID |
| problem_id | BIGINT UNSIGNED | 题目 ID |
| status | TINYINT | 0未开始，1尝试过，2已通过 |
| attempt_count | INT | 尝试次数 |
| first_submit_time | DATETIME | 首次提交时间 |
| first_ac_time | DATETIME | 首次通过时间 |
| last_submit_time | DATETIME | 最近提交时间 |
| solve_source | VARCHAR(20) | MANUAL / CODEFORCES / NOWCODER |
| accepted_submission_id | BIGINT UNSIGNED | 通过的提交记录 ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### post

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| author_user_id | BIGINT UNSIGNED | 作者 ID |
| problem_id | BIGINT UNSIGNED | 关联题目 ID |
| training_plan_id | BIGINT UNSIGNED | 关联训练计划 ID |
| post_type | VARCHAR(20) | DISCUSSION / SOLUTION / HELP / NOTICE |
| title | VARCHAR(255) | 帖子标题 |
| content_md | MEDIUMTEXT | Markdown 内容 |
| status | TINYINT | 0删除，1正常 |
| is_pinned | TINYINT | 0不置顶，1置顶 |
| accepted_comment_id | BIGINT UNSIGNED | 采纳的评论 ID（仅用于 HELP） |
| view_count | INT UNSIGNED | 浏览数 |
| like_count | INT UNSIGNED | 点赞数 |
| comment_count | INT UNSIGNED | 评论数 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

业务规则：
- NOTICE 只允许管理员发布
- accepted_comment_id 只用于 HELP 类型帖子
- 作者可管理自己的帖子，管理员可管理全部帖子
- 删除采用软删除（status=0），不物理删除

### post_comment

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| post_id | BIGINT UNSIGNED | 所属帖子 ID |
| user_id | BIGINT UNSIGNED | 评论者 ID |
| parent_id | BIGINT UNSIGNED | 所属一级评论 ID，NULL 表示一级评论 |
| reply_to_user_id | BIGINT UNSIGNED | 回复目标用户 ID |
| content | TEXT | 评论内容 |
| status | TINYINT | 0删除，1正常 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

评论两级结构：
- 一级评论：parent_id 为 NULL
- 回复：parent_id 指向所属一级评论 ID
- reply_to_user_id 表示具体回复哪个用户
- 回复时必须校验 parent_id 属于同一帖子
- 不支持无限层级评论树

### post_like

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| post_id | BIGINT UNSIGNED | 帖子 ID |
| user_id | BIGINT UNSIGNED | 用户 ID |
| create_time | DATETIME | 点赞时间 |

唯一索引 (post_id, user_id) 保证用户不能重复点赞同一帖子。

### oj_account

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| user_id | BIGINT UNSIGNED | 用户 ID |
| platform | VARCHAR(20) | CODEFORCES / NOWCODER |
| external_user_id | VARCHAR(128) | 外部平台用户标识 |
| display_name | VARCHAR(128) | 外部平台显示名 |
| verify_status | TINYINT | 0待审核，1已验证，2已拒绝 |
| sync_enabled | TINYINT | 0禁用同步，1启用同步 |
| last_sync_cursor | VARCHAR(128) | 上次同步游标 |
| last_sync_time | DATETIME | 上次同步时间 |
| last_sync_success | TINYINT | 0失败，1成功 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### oj_submission

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| oj_account_id | BIGINT UNSIGNED | OJ 账号 ID |
| user_id | BIGINT UNSIGNED | 用户 ID |
| platform | VARCHAR(20) | 平台 |
| remote_submission_id | VARCHAR(64) | 外部平台提交 ID（VARCHAR，不假设都是数字） |
| problem_id | BIGINT UNSIGNED | 本地题目 ID |
| external_problem_key | VARCHAR(64) | 外部题目标识 |
| verdict | VARCHAR(32) | 评测结果 |
| language | VARCHAR(64) | 编程语言 |
| submitted_time | DATETIME | 提交时间 |
| is_first_ac | TINYINT | 是否首次通过 |
| create_time | DATETIME | 创建时间 |

### sync_task_log

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | 自增主键 |
| oj_account_id | BIGINT UNSIGNED | OJ 账号 ID |
| platform | VARCHAR(20) | 平台 |
| trigger_type | VARCHAR(20) | SCHEDULED / MANUAL |
| task_status | VARCHAR(20) | RUNNING / SUCCESS / FAILED |
| cursor_before | VARCHAR(128) | 同步前游标 |
| cursor_after | VARCHAR(128) | 同步后游标 |
| fetched_count | INT | 拉取条数 |
| inserted_count | INT | 新插入条数 |
| first_ac_count | INT | 首次通过条数 |
| error_message | VARCHAR(1000) | 错误信息 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| create_time | DATETIME | 创建时间 |

## 设计决策

### 为什么不使用数据库外键

- 外键约束会增加写入开销
- 分库分表时外键会成为障碍
- 关联完整性由 Service 层保证
- 通过唯一索引和普通索引支持查询性能

### 为什么不创建角色表和排行榜表

- 角色：权限模型固定为 is_admin 字段，第一版无需 RBAC
- 排行榜：直接从 user_problem_status 表统计首次通过数量（first_ac_time IS NOT NULL），不需要额外的排行榜表

### 排行榜统计口径

以 user_problem_status 表中 first_ac_time 不为空的记录数为统计依据，即首次通过题目数量。不按提交次数统计。
