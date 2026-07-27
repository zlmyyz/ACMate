# IMPLEMENTATION STATUS

> Updated: 2026-07-27 | Discussion lifecycle completion

## Feature Status

| 模块 | PRD 要求 | 后端状态 | 前端状态 | 测试状态 | 联调状态 | 缺口 |
|---|---|---|---|---|---|---|---|
| 认证 | 注册/登录/Session/CSRF | 完成 | 完成 | 50 pass | 已联调 | — |
| 用户资料 | 当前用户/昵称/头像/简介 | 完成 | 完成 | 17 pass | 待联调 | CF账号/训练计划/题目列表(后续) |
| 公共题库 | 分页/筛选/仅ACTIVE | 完成 | 完成 | 23 pass | 待联调 | — |
| 题目详情 | 详情/权限/停用可见性 | 完成 | 完成 | 5 pass | 待联调 | — |
| 创建题目 | 表单/CSRF/查重 | 完成 | 完成 | 4 pass | 待联调 | — |
| 编辑题目 | 编辑/权限/draft | 完成 | 完成 | 5 pass | 待联调 | — |
| 我的题目 | 状态筛选/停用/恢复 | 完成 | 完成 | 23 pass | 待联调 | — |
| 管理员题目 | 全部/状态/停用原因/强制停用 | 完成 | 完成 | 9 pass | 待联调 | 审计日志待补充 |
| 用户主页 | 公开信息/统计/题目列表 | 完成 | 完成 | 12 pass | 待联调 | 题目/计划列表(后续) |
| 训练计划 | CRUD/类型/编排/进度 | 完成 | 完成 | 10 pass | 待联调 | 成员进度/赛时统计 |
| 训练成员进度 | 加入/移除/状态/备注 | 完成 | 加入/移除 | 部分 | 待联调 | 成员进度详情/赛时统计 |
| 帖子 | CRUD/类型/deactivation/权限/N+1/原子计数 | **已完成** | 完成 | 46 pass | 待联调 | — |
| 评论 | 一级评论/一层回复/停用追踪 | **已完成** | 完成 | 含 | 待联调 | — |
| 点赞 | 帖子点赞/原子计数/去重 | **已完成** | 完成 | 含 | 待联调 | — |
| 排行榜 | 总榜/7天/30天/分页 | 完成 | 完成 | 5 pass | 待联调 | 排名规则待调优 |
| CF账号 | 绑定/审核/同步/冷却 | 完成 | 完成 | 8 pass | 待联调 | **真实同步服务未实现** |
| 同步任务 | 手动/定时/日志/重试 | 完成 | 完成 | — | 待联调 | **真实CF API同步未实现** |
| 管理员用户管理 | 列表/禁用/改昵称/提权 | 完成 | 完成 | 8 pass | 待联调 | **禁用用户Session未失效** |
| 通知 | 7种场景/已读/未读 | 完成 | 完成 | — | 待联调 | **事件触发发送待实现** |
| 管理员内容管理 | 帖子/评论管理 | **已完成** | 完成 | — | 待联调 | 审计日志已集成 |
| 操作日志 | 管理员审计 | **已完成** | 完成 | — | 待联调 | 高风险操作写入已集成 |
| 数据导出 | CSV | 完成 | 完成 | — | 待联调 | 训练进度/成员数据导出 |
| 文件上传 | 头像/类型校验/安全 | 完成 | 完成 | 含 | 待联调 | — |

## 测试统计

| 层 | 测试数 | 通过 | 失败 |
|---|---|---|---|
| 后端 | 289 | 289 | 0 |
| 前端 | 110 | 109 | 1（预存超时） |

全部测试无需数据库。Problem*Test lambda cache 通过 MybatisPlusTestHelper 初始化 TableInfo 修复。SessionLogin 新增 4 个认证异常语义测试（DB 异常→500、密码错误→401、用户不存在→401、500 响应不泄露 SQL）。

## 数据库迁移

| 版本 | 内容 | 状态 |
|---|---|---|
| V1 | app_user表 | OK |
| V2 | problem/training_plan/training_plan_problem/user_problem_status表 | OK |
| V3 | post/post_comment/post_like表 | OK |
| V4 | oj_account/oj_submission/sync_task_log表 | OK |
| V5 | problem表增加停用审计字段 | OK |
| V6 | app_user表增加bio列 | OK |
| V7 | training_plan表重构 + training_plan_member表 | OK |
| V8 | notification表 | OK |
| V9 | post和post_comment增加停用审计字段 | **已修复**（移除非法 `IF NOT EXISTS` 语法） |
| V10 | audit_log表 | OK |
| V11 | app_user表增加 uk_app_user_nickname 唯一索引 | OK |

Flyway 在 Spring Boot 4.1.0 中无自动配置，通过手动 `FlywayConfig`（`@ConditionalOnProperty("spring.flyway.enabled")`）启用。现有数据库 baseline 在 V11，全新数据库 `acmate_fresh` 验证 V1-V11 完整迁移通过，重启幂等。未来 V12+ 对两种数据库均正常执行。

---

## 发布前审计 (2026-07-27)

### Git 状态

- 分支：`feat/vue-frontend`，有未提交变更（测试稳定 + 认证异常语义修复）
- 无硬编码密钥、密码、Token
- 无 `.env`、`.pem`、`.key` 文件追踪
- `.gitignore` 已补充 `uploads/` 和 `logs/`

### 安全审计

| 检查项 | 结果 | 备注 |
|---|---|---|
| CSRF 保护（写端点） | 通过 | 仅 login/register 豁免 |
| Admin 权限（Service 层双重检查） | 通过 | Controller + Service 双检 |
| 资源所有权校验 | 通过 | 创建者或管理员模式 |
| 文件上传路径穿越防护 | 通过 | normalize + startsWith + 扩展名白名单 |
| Markdown HTML 注入防护 | 通过 | `html: false` + 危险协议拦截 |
| CSV 公式注入防护 | **已修复** | 前缀 `=`, `+`, `-`, `@` 自动添加单引号 |
| 分页大小限制 | 通过 | `max(1, min(size, 100))` + MyBatis 兜底 |
| 错误响应无堆栈 | 通过 | 仅返回 code + message |
| Password/Token 不记日志 | 通过 | AuthenticatedUser.toString 排除 passwordHash |
| 外部 URL 危险协议 | 通过 | javascript:/data:/vbscript: 已拦截 |
| **登录 401 根因修复** | **已修复** | 数据库缺失 V6 `bio` 列 → SQL Unknown column → InternalAuthenticationServiceException → 全局异常处理器误吞为 401 |
| Flyway 一致性修复 | **已修复** | Spring Boot 4.1.0 无自动配置 → 手动 FlywayConfig；V9 ADD COLUMN IF NOT EXISTS 非法语法 → 移除 IF NOT EXISTS |
| 昵称大小写自更新 | **已修复** | 数据库 ci 排序规则下，大小写变体更新被误判 409 → equalsIgnoreCase |
| **认证异常语义** | **已修复** | 新增 `@ExceptionHandler(InternalAuthenticationServiceException)` → 500；`RestAuthenticationEntryPoint` 区分 `InternalAuthenticationServiceException`（500）和普通未登录（401） |
| MyBatis-Plus Lambda Cache | **已修复** | 3 个 Mockito-only 测试类缺失 entity 初始化 → 统一 `MybatisPlusTestHelper.initEntityTables()` |

### 认证错误状态码规则

| 场景 | HTTP | 响应 |
|---|---|---|
| 用户不存在 | 401 | `用户名或密码错误` |
| 密码错误 | 401 | `用户名或密码错误` |
| 账号已禁用 | 403 | `账号已被禁用` |
| 未登录访问受保护资源 | 401 | `未登录或登录已失效` |
| 数据库异常 / SQL 字段缺失 | 500 | `服务器内部错误，请稍后再试` |
| 认证基础设施异常 | 500 | `服务器内部错误，请稍后再试` |

500 响应不包含 SQL、表名、字段名或堆栈。

### V9 历史修改

V9 首次出现在 `bd56c20`（Phase 8），含非法 `ADD COLUMN IF NOT EXISTS` 语法。该语法从未被 Flyway 执行——`bd56c20` 提交时 Spring Boot 4.1.0 无 Flyway 自动配置，FlywayConfig 在 `0505f11` 才引入，且现有数据库在 V11 基线化。V9 修正是首次启用 Flyway 前对未发布迁移的语法修正，无 checksum 冲突风险。

### 已知阻塞项

| 问题 | 严重程度 | 说明 |
|---|---|---|
| Codeforces API 未集成 | 高 | 无 RestTemplate/WebClient/HttpURLConnection；无 @Scheduled 定时同步 |
| 通知事件触发未实现 | 中 | 通知 CRUD 已就绪，但 7 种场景事件触发代码不存在 |
| 禁用用户 Session 仍有效 | 中 | AdminUserService.toggleStatus 只改数据库，不失效已登录 Session |
| 操作日志未写入 | **已修复** | AdminContentServiceImpl 停用/恢复操作已调用 AuditLogService |

### Codeforces 能力审计

| 能力 | PRD 要求 | 当前状态 |
|---|---|---|
| CF handle 绑定 | 是 | 仅数据库存储，无 handle 真实性校验 |
| CF API 提交记录同步 | 是 | 未实现 — 无 RestTemplate 或 HttpURLConnection 调用 |
| 定时同步 (cron) | 是 | 未实现 — 无 @Scheduled 或 @EnableScheduling |
| 同步冷却期 | 是 | 未实现 |
| 同步任务日志 | 是 | sync_task_log 表就绪，仅手动状态查询 |
| 同步重试机制 | 是 | 未实现 |
| 审核流程 | 是 | verifyStatus 字段存在，仅管理员手动更改 |
| 排行榜数据源 | 是 | oj_submission 表聚合查询已就绪，缺真实数据填充 |
