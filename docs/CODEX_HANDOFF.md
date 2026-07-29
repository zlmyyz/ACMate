# ACMate 项目交接文档

生成时间：2026-07-29 | 最后更新：2026-07-29

---

## 1. 当前 Git 状态

### 分支
- 当前：`feat/vue-frontend`（仅此分支有未提交变更）
- 其他分支（已合并/过时）：`feat/problem-management`, `feat/session-auth`, `feat/user-registration`, `main`

### HEAD commit
```
1acd7b4 feat: complete admin audit log workflow
```

### 最近 10 个 commit（从新到旧）
```
1acd7b4 feat: complete admin audit log workflow
f3ae145 docs: update devlog with admin user management entry
6a15e6e feat: complete admin user management workflow
f76bbf1 docs: mark trusted leaderboard verification pending
730622f fix: switch leaderboard queries to oj_first_ac with disabled-user filter
2adc50d fix: finalize Codeforces sync reliability and verification
be8175d feat: implement Codeforces submission sync with idempotent AC tracking
c6831da feat: add problem selection to training plans
85f5914 docs: synchronize verified workflows and training gaps
977eb9a fix: complete browser-verified core workflows
```

### 工作区状态（本轮变更后）

```
?? .codex/                               # Codex 配置目录，不提交
```

本轮变更包含：服务端同步冷却、每日定时同步、排行榜并列排序、前后端测试、文档更新。

### 工作区状态（Chromium 验收后最终状态）

```
?? .codex/                               # Codex 配置目录，不提交
```

所有业务变更已纳入 `1acd7b4` 提交，无未提交变更。审计日志前端/后端代码、测试、文档均已提交。

---

## 2. 项目当前完成模块

### 2.1 题库

| 项目 | 状态 |
|------|------|
| 功能 | 公共列表/详情/创建/编辑/我的题目/停用恢复/管理员全站题库 + 停用原因 + 强制停用 |
| 后端测试 | 155（ProblemQuery:23, ProblemCommand:64, AdminProblemQuery:9, ProblemController:59） |
| 前端测试 | ~56（problems.test.ts + 部分 phases567） |
| 浏览器验收 | 已验收（创建流程、详情、权限按钮包括管理员+创建者双重身份） |
| 待办 | 无功能待办，物理删除未实现（当前不做） |

### 2.2 讨论区

| 项目 | 状态 |
|------|------|
| 功能 | 帖子 CRUD、6 种类型、一级评论与回复、点赞/取消点赞、原子计数、CREATOR/ADMIN 停用追踪、N+1 批量加载修复、HTML 注入拦截 |
| 后端测试 | 57（DiscussionServiceImplTest） |
| 前端测试 | 无独立测试文件（依赖于 Vue 页面） |
| 浏览器验收 | 已验收（发帖、评论、点赞、停用/恢复、权限） |
| 待办 | HELP 帖子采纳评论未实现（PRD 提及，非当前阻塞） |

### 2.3 训练计划

| 项目 | 状态 |
|------|------|
| 功能 | CRUD、PUBLIC/PERSONAL 类型、时间状态自动计算、题目批量编排（create 含 problemIds + 独立 updateProblems）、成员加入/移除、通知集成 |
| 后端测试 | 52（TrainingPlanServiceImplTest） |
| 前端测试 | ~16（training.test.ts + 部分 phases567） |
| 浏览器验收 | 已验收（创建/编辑/详情/加入/题目管理/停用恢复） |
| 待办 | 成员进度详情页、赛时统计快照未实现 |

### 2.4 通知系统

| 项目 | 状态 |
|------|------|
| 功能 | 11 种事件型通知、@TransactionalEventListener(AFTER_COMMIT) 可靠投递、200/chunk 批量插入、Pinia 轮询（30s）、可见性暂停/恢复、unreadOnly 过滤、自通知抑制、99+ badge |
| 后端测试 | 42（NotificationServiceImpl:34 + NotificationEventListener:8） |
| 前端测试 | 39（notifications.test.ts） |
| 浏览器验收 | 已验收（11 种事件全部端到端验证、自通知抑制、已读/未读、权限） |
| 待办 | 无 WebSocket 推送（当前不要求）；无邮件/短信通知（当前不做） |

### 2.5 Codeforces 同步

| 项目 | 状态 |
|------|------|
| 功能 | CF handle 绑定/解绑/审核、cursor 分页增量同步、DB 级 first-AC 原子约束（oj_first_ac UNIQUE KEY）、幂等、同步任务日志、**服务端 1h 冷却**、**每日定时同步** |
| 后端测试 | 43（OjAccountServiceImplTest） |
| 前端测试 | ~11（oj.test.ts + 部分 phases567） |
| 浏览器验收 | 已验收（绑定/解绑/审核/同步按钮/结果展示/冷却提示）—— 但**真实 CF API 端到端验收待进行** |
| 待办 | 冷却期结束前返回 COOLDOWN SyncResult（含剩余秒数）；@EnableScheduling + @Scheduled(cron="0 0 * * * *") 每小时整点，支持 enabled/cron/zone 配置覆盖 |

### 2.6 可信排行榜

| 项目 | 状态 |
|------|------|
| 功能 | 总榜/7d/30d 分页、数据源为 oj_first_ac + VERIFIED 账号、禁用用户 SQL 层排除、未映射本地题目的 AC 计入、page/size 分页、isMe 标记、**并列排序（solved_count DESC → last_accepted_time ASC → user_id ASC）** |
| 后端测试 | 12（LeaderboardServiceImplTest 7个 + 含在 OjAccountServiceImplTest 中） |
| 前端测试 | ~7（phases567.test.ts 含 lastAcceptedTime 列） |
| 浏览器验收 | 待进行（依赖 CF 真实同步验收后才有真实数据填充） |
| 待办 | 依赖 CF 真实同步才能填充数据做真实验证 |

### 2.7 管理员用户管理

| 项目 | 状态 |
|------|------|
| 功能 | 用户列表（keyword/status/admin 筛选）、停用（原因必填 + 最后管理员保护 + 自操作保护 + Session 失效 + 审计日志）、恢复、授予管理员、撤销管理员（最后管理员保护 + 自操作保护 + Session 失效） |
| 后端测试 | 49（AdminUserServiceImplTest:31 + AdminUserControllerTest:18） |
| 前端测试 | 8（phases567.test.ts AdminUsersView 部分） |
| 浏览器验收 | 核心停用/恢复/授权/撤权流程已通过真实 Chromium 验收 |
| 待办 | 被停用用户与角色变更后的旧 Session 失效、其他用户 Session 不受影响仍待专项验收 |

### 2.8 操作日志（审计日志）

| 项目 | 状态 |
|------|------|
| 功能 | 14 种标准化 actionType 常量 + 6 种 targetType + 白名单校验、完整过滤接口（actionType/targetType/targetId/actorKeyword/startTime/endTime + 分页 + 稳定排序）、批量 actor 加载防 N+1、所有高风险操作写日志全覆盖、前端页面（URL 同步 + 筛选 + 状态变更列） |
| 后端测试 | 25（AuditLogServiceImplTest:16 + AuditLogControllerTest:9） |
| 前端测试 | 7（audit-logs.test.ts） |
| Chromium 验收 | **已通过** — 14 种 actionType 全部通过真实 API 生成并验证、页面加载/筛选/分页/操作人/状态变更/原因显示均正确 |
| 待办 | 无功能待办 |

---

## 3. 当前未完成事项

| # | 事项 | 关键文件 | 优先级 |
|---|------|----------|--------|
| 1 | 排行榜真实数据验证 | 无代码修改，纯验收 | 中 |
| 2 | 管理员用户 Session/角色旧 Session 专项验收 | 无代码修改，纯验收 | 中 |
| 4 | CF 定时同步未启用（@Scheduled） | 需在配置类加 @EnableScheduling + cron 表达式 | 低 |
| 5 | 同步冷却期服务端未强制 | `OjAccountController` 或 `ServiceImpl` 加时间检查 | 低 |
| 7 | 训练计划成员进度详情页 | 后端计划中，尚未实现 | 未来 |
| 8 | 训练计划赛时统计快照 | 后端计划中，尚未实现 | 未来 |

---

## 4. 当前测试基线

### 后端
```
Tests run: 518, Failures: 0, Errors: 0, Skipped: 0
```
- 全部纯 Mockito / @WebMvcTest，无需数据库
- `MybatisPlusTestHelper.initEntityTables()` 初始化 MyBatis-Plus lambda 缓存
- CF 同步 43 个测试覆盖同步、幂等、first-AC、上游失败、冷却、concurrent 保护

### 前端
```
Test Files: 9 passed, 9 total
Tests: 191 passed, 191 total
```
- vitest + @vue/test-utils + jsdom
- 9 个测试文件：含冷却展示测试、排行榜 lastAcceptedTime 列测试

### type-check / lint / build
| 检查 | 结果 |
|------|------|
| type-check (`vue-tsc --build`) | pass |
| lint (`oxlint + eslint`) | pass |
| build (`vite build`) | pass |

---

## 5. 数据库状态

### Flyway 迁移文件
```
V1__create_app_user.sql
V2__create_problem_training_tables.sql
V3__create_discussion_tables.sql
V4__create_oj_sync_tables.sql
V5__add_problem_deactivation_audit.sql
V6__add_user_bio.sql
V7__restructure_training_plan.sql
V8__create_notification.sql
V9__add_post_comment_audit.sql
V10__create_audit_log.sql
V11__add_nickname_unique.sql
V12__add_training_plan_deactivation.sql
V13__restructure_notification.sql
V14__add_first_ac_constraint.sql
```

### Baseline 情况
- 现有数据库 `acmate`：baseline 在 **V11**（`flyway_schema_history` 有 1 条 BASELINE 条目，version=11）
- V1-V10 从未在现有库执行（baseline 跳过了它们）
- V14（oj_first_ac 表）已执行，当前 schema version 为 **14**

### 已知迁移注意事项
1. **Spring Boot 4.1.0 无 Flyway 自动配置** — 通过手动 `FlywayConfig`（`@ConditionalOnProperty("spring.flyway.enabled")`）启用
2. **V9 历史**：该迁移最初含 MySQL 不支持的 `ADD COLUMN IF NOT EXISTS` 语法（MariaDB 语法），已在 `0505f11` 提交中移除。该语法从未被执行过（Flyway 在 Spring Boot 4.1.0 中无自动配置，`FlywayConfig` 是后续才引入的）
3. **现有库 V11+ 可直接创建 V14 迁移**，建表脚本安全重复执行（CREATE TABLE IF NOT EXISTS）

---

## 6. 下一步推荐任务

### 高优先级
1. **CF 真实 API + 真实排行榜数据验收** — 用真实 CF 账号端到端验证同步（冷却/定时/手动）、排行榜数据填充（并列排序/分页/去重）

### 中优先级
2. **管理员用户 Session 专项验收** — 验证停用及角色变化后的旧 Session 失效

### 低优先级
3. **牛客平台支持** — 新增 OJ 平台枚举和对应的 API 客户端

---

## 7. 禁止操作

1. **不修改已执行的 Flyway 迁移** — 已执行到 V13，V14 未执行。任何对 V1-V13 的修改会导致 checksum 不匹配，Flyway 拒绝启动
2. **不提交敏感文件** — `.env`、`application-local.yml`、`*.pem`、`*.key`、`cookies`、`sessions`、`CSRF tokens`、日志文件、`node_modules/`、`.codex/`
3. **不执行危险 git 命令** — 不经确认不执行 `git push --force`、`git reset --hard`、`git rebase -i`、`git checkout -- .`、`git clean -f`、`git branch -D`
4. **不跳过 pre-commit hooks** — 任何时候不使用 `--no-verify` / `--no-gpg-sign`
5. **不修改历史 commit** — 当前分支有未提交变更，在已提交 commit 基础上新增 commit，不 `--amend`
