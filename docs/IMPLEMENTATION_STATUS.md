# IMPLEMENTATION STATUS

> Updated: 2026-07-27 | Pre-release audit

## Feature Status

| 模块 | PRD 要求 | 后端状态 | 前端状态 | 测试状态 | 联调状态 | 缺口 |
|---|---|---|---|---|---|---|---|
| 认证 | 注册/登录/Session/CSRF | 完成 | 完成 | 50 pass | 已联调 | — |
| 用户资料 | 当前用户/昵称/头像/简介 | 完成 | 完成 | 12 pass | 待联调 | CF账号/训练计划/题目列表(后续) |
| 公共题库 | 分页/筛选/仅ACTIVE | 完成 | 完成 | 23 pass | 待联调 | — |
| 题目详情 | 详情/权限/停用可见性 | 完成 | 完成 | 5 pass | 待联调 | — |
| 创建题目 | 表单/CSRF/查重 | 完成 | 完成 | 4 pass | 待联调 | — |
| 编辑题目 | 编辑/权限/draft | 完成 | 完成 | 5 pass | 待联调 | — |
| 我的题目 | 状态筛选/停用/恢复 | 完成 | 完成 | 23 pass | 待联调 | — |
| 管理员题目 | 全部/状态/停用原因/强制停用 | 完成 | 完成 | 9 pass | 待联调 | 审计日志待补充 |
| 用户主页 | 公开信息/统计/题目列表 | 完成 | 完成 | 12 pass | 待联调 | 题目/计划列表(后续) |
| 训练计划 | CRUD/类型/编排/进度 | 完成 | 完成 | 10 pass | 待联调 | 成员进度/赛时统计 |
| 训练成员进度 | 加入/移除/状态/备注 | 完成 | 加入/移除 | 部分 | 待联调 | 成员进度详情/赛时统计 |
| 帖子 | CRUD/类型/Markdown/点赞 | 完成 | 完成 | 24 pass | 待联调 | — |
| 评论 | 一级评论/一层回复 | 完成 | 完成 | 含 | 待联调 | — |
| 点赞 | 帖子点赞/去重 | 完成 | 完成 | 含 | 待联调 | — |
| 排行榜 | 总榜/7天/30天/分页 | 完成 | 完成 | 5 pass | 待联调 | 排名规则待调优 |
| CF账号 | 绑定/审核/同步/冷却 | 完成 | 完成 | 8 pass | 待联调 | **真实同步服务未实现** |
| 同步任务 | 手动/定时/日志/重试 | 完成 | 完成 | — | 待联调 | **真实CF API同步未实现** |
| 管理员用户管理 | 列表/禁用/改昵称/提权 | 完成 | 完成 | 8 pass | 待联调 | **禁用用户Session未失效** |
| 通知 | 7种场景/已读/未读 | 完成 | 完成 | — | 待联调 | **事件触发发送待实现** |
| 管理员内容管理 | 帖子/评论管理 | 完成 | 完成 | — | 待联调 | — |
| 操作日志 | 管理员审计 | 完成 | 完成 | — | 待联调 | **高风险操作写入待集成** |
| 数据导出 | CSV | 完成 | 完成 | — | 待联调 | 训练进度/成员数据导出 |
| 文件上传 | 头像/类型校验/安全 | 完成 | 完成 | 含 | 待联调 | — |

## 测试统计

| 层 | 测试数 | 通过 | 失败 |
|---|---|---|---|
| 后端 | 214 | 214 | 0 |
| 前端 | 107 | 107 | 0 |

所有测试无需数据库 — 后端 @WebMvcTest 使用 Mockito，前端使用 Vitest + vi.mock。

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
| V9 | post和post_comment增加停用审计字段 | OK |
| V10 | audit_log表 | OK |

迁移使用 Flyway 管理，V1-V10 均为幂等 SQL（IF NOT EXISTS / IF EXISTS）。未经 MySQL 8 实际验证。

---

## 发布前审计 (2026-07-27)

### Git 状态

- 分支：`feat/vue-frontend`，工作区干净
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

### 已知阻塞项

| 问题 | 严重程度 | 说明 |
|---|---|---|
| Codeforces API 未集成 | 高 | 无 RestTemplate/WebClient/HttpURLConnection；无 @Scheduled 定时同步 |
| 通知事件触发未实现 | 中 | 通知 CRUD 已就绪，但 7 种场景事件触发代码不存在 |
| 禁用用户 Session 仍有效 | 中 | AdminUserService.toggleStatus 只改数据库，不失效已登录 Session |
| 操作日志未写入 | 低 | AuditLogService.log() 方法存在但未被调用 |
| 迁移未经 MySQL 8 验证 | 低 | 本地无 MySQL 8，SQL 语法差异无法排除 |

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
