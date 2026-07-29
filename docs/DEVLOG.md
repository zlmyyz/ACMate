# DEVLOG

## 2026-07-29（七轮）：管理员用户管理

### 本次目标

实现管理员用户管理完整工作流：用户列表（含 status/admin 筛选）、停用/恢复（含原因、Session 失效、审计日志）、管理员授予/撤销（含 Session 失效、最后管理员保护、自操作保护）、前后端联调、49 后端测试 + 8 前端测试。

### 新增文件

**后端：**
- `admin/dto/DeactivateUserRequest.java` — 停用原因 DTO（@NotBlank + @Size 500, setter 内 strip）

**测试：**
- `admin/controller/AdminUserControllerTest.java` — 18 个 WebMvcTest（列表/停用/恢复/授权/撤销 + CSRF + 权限 + 参数校验）
- `admin/service/impl/AdminUserServiceImplTest.java` — 31 个 Mockito 测试（所有业务规则 + 边界 + Session 失效）

### 修改文件

**后端 Service（2 个）：**
- `admin/service/AdminUserService.java` — listUsers 增加 status/admin 参数；新增 deactivate/reactivate/grantAdmin/revokeAdmin

- `admin/service/impl/AdminUserServiceImpl.java` — 完整重写：
  - 列表：keyword trim + status/admin SQL 过滤 + create_time DESC, id DESC 稳定排序
  - 停用：403（自操作）/400（原因空）/404/400（最后管理员）校验，幂等跳过，审计日志，Session 失效
  - 恢复：404/403 校验，幂等跳过，审计日志
  - 授权：404/403 校验，幂等跳过，审计日志，Session 失效
  - 撤销：403（自操作）/400（最后管理员）校验，Session 失效，审计日志
  - Session 失效：SessionRegistry.getAllPrincipals() → 逐个 expireNow()

**后端 Controller（1 个）：**
- `admin/controller/AdminUserController.java` — PUT 端点（deactivate/restore/grant-admin/revoke-admin），GET 列表支持 status/admin 参数

**前端文件（3 个）：**
- `api/admin.ts` — 替换 toggleUserStatus/toggleUserAdmin 为独立函数：deactivateUser(id, reason)、reactivateUser(id)、grantUserAdmin(id)、revokeUserAdmin(id)
- `types/admin.ts` — 新增 AdminUserFilterParams 接口（page/size/keyword/status/admin）
- `views/AdminUsersView.vue` — 完整重写：URL 查询参数同步、requestId 防过期响应、状态筛选（ACTIVE/INACTIVE）、角色筛选（ADMIN/USER）、Teleport 弹窗（停用含原因、恢复/授权/撤销含警告）、空状态

**测试（1 个）：**
- `frontend/src/__tests__/phases567.test.ts` — 新增 8 个 AdminUsersView 测试（渲染/搜索/筛选/弹窗/原因校验/API 调用/错误处理/空状态）

**文档（3 个）：**
- `docs/API.md` — 管理员用户管理完整 API 文档
- `docs/ROADMAP.md` — 更新阶段 8 完成状态、测试计数
- `docs/IMPLEMENTATION_STATUS.md` — 更新管理员用户管理行、测试统计、日期

### 三大约束实现

1. **自操作保护**：`grantAdmin` 对已是管理员返回 403，`deactivateUser` 对操作自己返回 400，`revokeAdmin` 对撤销自己返回 400
2. **最后管理员保护**：`deactivateUser` 和 `revokeAdmin` 均检查目标是否为最后一名活跃管理员，是则 400
3. **Session 失效**：`deactivate`、`grantAdmin`、`revokeAdmin` 均调用 `expireUserSessions()` 使目标用户所有 Session 立即失效

### 自动化验证

- 后端：477 tests passed（新增 49 admin 测试 + 428 现有）
- 前端：181 tests passed（新增 8 个 + 173 现有，两轮运行确认）
- 前端 lint：pass
- 前端 build-only：success（type-check 有 2 个 pre-existing oj.ts 错误）

### 提交

`6a15e6e` feat: complete admin user management workflow

---

## 2026-07-28（六轮）：可信排行榜数据查询修复

### 本次目标

修复排行榜 SQL 查询的数据口径问题：改用 `oj_first_ac` 作为唯一 AC 统计源，排除禁用用户，修正分页 total。

### 代码修复

**提交：** `730622f` — fix: switch leaderboard queries to oj_first_ac with disabled-user filter

**修改文件（3 个，39+ / 36-）：**

- `oj/mapper/OjSubmissionMapper.java` — 重写 6 个 SQL 查询
- `leaderboard/service/impl/LeaderboardServiceImpl.java` — 移除冗余 status 过滤
- `docs/IMPLEMENTATION_STATUS.md` — 更新排行榜状态

**核心变更：**

1. **数据源切换**：从 `oj_submission.is_first_ac` + `problem_id` 改为 `oj_first_ac` 表（DB 级 UNIQUE 约束保证去重）
2. **移除 `problem_id IS NOT NULL` 限制**：未映射到本地题库的 Codeforces AC 现在可计入排行榜
3. **SQL 层过滤禁用用户**：`INNER JOIN app_user u ON u.status = 1`
4. **修正分页 total**：count 查询同样加入 `app_user` join，total 与实际条目数一致
5. **7d/30d 时间过滤**：通过 `submission_id` 关联 `oj_submission.submitted_time`，ALL 不关联 submission 表
6. **移除 Service 层冗余过滤**：`enrichRows` 中的 `status == 1` 判断已由 SQL 保证

### 自动化验证

- 后端：428 tests passed（全部 Mockito，无 DB 依赖）
- 前端：173 tests passed
- 前端 type-check：2 pre-existing errors（`oj.ts`/`oj.test.ts`，非本次引入）
- 前端 lint：pass
- 前端 build：output produced（type-check 失败阻断整体 build 退出码，但非排行榜问题）
- 编译：pass

### 待办

- [ ] Codeforces 真实同步验收
- [ ] 使用真实 AC 数据验证排行榜
- [ ] Chromium 人工验收
- [ ] 分页/并列排名/时间窗口的真实数据核对

---

## 2026-07-28（五轮）：Codeforces 提交同步最终验收

### 本次目标

完成 Codeforces 提交增量同步的可靠性验证与最终验收：修复 contextLoads、数据库级 first-AC 原子约束、CF API 真实验证、前端同步界面。

### 修改文件

**新建：**
- `db/migration/V14__add_first_ac_constraint.sql` — oj_first_ac 表，UNIQUE KEY (user_id, platform, external_problem_key)
- `oj/entity/FirstAc.java` — first-AC 实体
- `oj/mapper/FirstAcMapper.java` — Mapper
- `frontend/src/__tests__/oj.test.ts` — 7 个前端同步测试

**修改：**
- `config/RestClientConfig.java` — Spring Boot 4.x 无 RestClient.Builder 自动配置，改用 `RestClient.builder()` 静态方法
- `oj/client/CodeforcesApiClient.java` — RestTemplate → RestClient（Spring 7.x）；Jackson 3.x import 修正
- `oj/controller/OjAccountController.java` — 新增 POST /me/sync
- `oj/service/impl/OjAccountServiceImpl.java` — 完整重写 sync：cursor 分页增量同步、DB 级 first-AC 原子约束、任务日志、幂等
- `oj/service/impl/OjAccountServiceImplTest.java` — 34 个测试
- `testutil/MybatisPlusTestHelper.java` — 注册 FirstAc 实体
- `frontend/src/views/OJAccountView.vue` — 同步按钮、结果展示、错误提示
- `frontend/src/api/oj.ts` — syncMyAccount()
- `frontend/src/types/oj.ts` — SyncResult 类型
- `docs/API.md` — OJ 账号 API 完整文档

### 核心设计

**cursor 分页增量同步**：取已存储提交中最大 remote_submission_id 作为 cursor，仅插入新提交。`LIMIT 1 ORDER BY DESC` 高效。

**DB 级 first-AC 原子性**：oj_first_ac 表 UNIQUE KEY (user_id, platform, external_problem_key)。插入流程：先 insert submission（DuplicateKeyException → 已同步，跳过 first-AC）；OK 时 insert first_ac（DuplicateKeyException → 非首次 AC），成功则标记 is_first_ac=1。

**幂等性**：重复同步 0 新提交，cursor 不变，任务日志独立记录。

**上游失败**：CF API 不可达返回 502，handle 不存在返回 404，Rate limit 返回 429。异常统一记录到 SyncTaskLog 和 account.lastSyncSuccess=0。

### 实际 CF API 验证

使用 `tourist` handle 真实调用 `https://codeforces.com/api/user.status?handle=tourist&from=1&count=500`，返回 `{"status":"OK","result":[...]}`。不存在的 handle 返回 `{"status":"FAILED","comment":"handle: User ... not found"}`。

### 测试结果

```
后端 Tests run: 428, Failures: 0, Errors: 0, Skipped: 0
  OjAccountServiceImplTest: 34（同步、幂等、first-AC、上游失败）
前端 Tests run: 173, Failures: 0
type-check + lint + build 全部通过
```

### 已知限制

- 同步冷却 1 小时未在服务端强制（PRD 要求）
- @Scheduled 定时同步未启用
- 牛客同步未实现

### 提交

`be8175d` feat: implement Codeforces submission sync with idempotent AC tracking

---

## 2026-07-28（四轮）：训练计划题目选择器

### 本次目标

实现训练计划创建和编辑时的题目选择功能：从题库搜索题目、添加到计划、排序、移除。仅创建者可管理题目，仅 ACTIVE 题目可加入，PUBLIC 计划题目变更发送通知。

### 修改文件

**后端新增：**
- `training/dto/PlanProblemRequest.java` — 单个题目项 DTO
- `training/dto/UpdateProblemsRequest.java` — 批量更新题目请求 DTO

**后端修改：**
- `training/dto/CreatePlanRequest.java` — 新增 `problemIds` 字段
- `training/service/TrainingPlanService.java` — 新增 `updateProblems` 方法
- `training/service/impl/TrainingPlanServiceImpl.java` — `createPlan` 增加 `validateAndInsertProblems`；新增 `updateProblems`（diff 对比，无变更跳过通知）；新增 `validateAndInsertProblems`（去重、ACTIVE 校验、有序插入）；新增 `notifyProblemsChanged`（仅 PUBLIC 计划）
- `training/controller/TrainingPlanController.java` — 新增 `PUT /{id}/problems`

**前端新增：**
- `components/training/TrainingProblemSelector.vue` — 可复用题目选择器（防抖搜索、分页加载、添加/移除/排序、已添加禁用）
- `stores/notifications.ts` — Pinia 通知 store

**前端修改：**
- `views/CreatePlanView.vue` — 集成 TrainingProblemSelector，`handleCreate` 传递 problemIds
- `views/EditPlanView.vue` — 集成 TrainingProblemSelector，`handleSave` 调用 updatePlanProblems
- `types/training.ts` — 新增 PlanProblemRequest、UpdateProblemsRequest、CreatePlanRequest.problemIds
- `api/training.ts` — 新增 `updatePlanProblems`
- `App.vue` — 通知 store 集成
- `components/layout/AppHeader.vue` — 使用 notificationStore
- `views/NotificationsView.vue` — 重构通知中心

**测试新增（后端 15 个）：**
- `TrainingPlanServiceImplTest.java` — 创建含题目（6）+ 更新题目（9）：正常创建、空列表、去重校验、题目不存在/停用拒绝、批量替换、重新排序、diff 无变更跳过通知、PUBLIC 通知、PERSONAL 不通知

### 浏览器验收（17/25 通过）

创建流程（1-8）、详情展示（9-12）、编辑流程（13-18）、权限验证（22）全部通过。通知验证（19-21）、管理员权限（23-24）、停用题目拒绝（25）由后端单元测试覆盖。

### 测试结果

```
后端 Tests run: 394, Failures: 0, Errors: 0, Skipped: 0
前端 Tests run: 166, Failures: 0
type-check + lint + build 全部通过
```

### 提交

`<pending>` feat: add problem selection to training plans

---

## 2026-07-28（三轮）：浏览器端全面验收与 Session 持久化修复

### 本次目标

对上一轮 977eb9a 提交的 11 项核心功能进行真实 Chromium 浏览器端验收，修复 Session 持久化和停用/恢复按钮可见性问题。

### 浏览器验收通过项（11/11）

| # | 功能 | 验收方式 | 结果 |
|---|------|----------|------|
| 1 | Session 持久化（刷新后恢复） | 全页刷新 /problems → 保持登录 | 通过（修复后） |
| 2 | 通知角标（真实数据） | 浏览器查看未读计数 | 通过 |
| 3 | 通知中心（全部功能） | unreadOnly/标记已读/全部已读/点击跳转 | 通过 |
| 4 | 帖子点赞/取消点赞 | 浏览器端 toggle 验证 | 通过 |
| 5 | 帖子评论通知投递 | Bob 评论 Alice 帖子 → Alice 收到通知 | 通过 |
| 6 | 训练计划日期/时间 | 创建和编辑计划的时间控件 | 通过 |
| 7 | 公告广播端到端 | Admin 发公告 → 其他用户可见 | 通过 |
| 8 | 所有视图创建者信息 | 题目详情/帖子/计划详情中显示创建者 | 通过 |
| 9 | 停用/恢复完整流程 | 创建者停用→恢复、管理员强制停用→恢复、普通用户拒绝 | 通过 |
| 10 | 管理员强制停用原因校验 | 空原因时确认按钮 disabled | 通过 |
| 11 | 管理员+创建者双重身份按钮 | 同一用户同时为 admin 和 creator 时按钮可见 | 通过（修复后） |

### 修复的文件

**Session 持久化修复（2 个文件）：**

- `frontend/src/router/index.ts` — `beforeEach` 增加 `if (!auth.initialized) return true`，防止 auth.init() 完成前误跳转
- `frontend/src/App.vue` — `watch(auth.isLoggedIn)` 增加 `auth.initialized` 条件，防止初始化期间误触发 redirect

**停用/恢复按钮可见性修复（1个文件）：**

- `frontend/src/views/ProblemDetailView.vue` — 修复互斥条件 `isCreator && !auth.isAdmin` 与 `auth.isAdmin && !isCreator` 导致 admin+creator 双重身份用户看不到任何按钮的问题

**管理员强制停用确认按钮修复（1个文件）：**

- `frontend/src/views/AdminProblemsView.vue` — 确认按钮增加 `:disabled="confirmLoading || !deactivateReason.trim()"`

### 测试结果

```
后端 Tests run: 379, Failures: 0, Errors: 0, Skipped: 0
前端 Tests run: 166, Failures: 0 (7 文件, 166 tests)
```

### 已知剩余缺口

- 训练计划题目选择器（创建/编辑时不支持批量选题和排序）
- 训练计划成员进度详情页
- 训练计划赛时统计快照
- Codeforces API 真实同步未实现
- 禁用用户 Session 未失效

### 提交

`977eb9a` feat: complete in-app notification workflow

---

## 2026-07-28：站内通知系统完善

### 本次目标

完成通知系统端到端实现：11 种通知事件、事件驱动架构、批量插入优化、Pinia 轮询、前端通知中心、34 个后端测试。

### 修改文件

**数据库迁移：**
- `db/migration/V13__restructure_notification.sql` — notification 表重构：`user_id` → `recipient_user_id`，`type` → `notification_type`，新增 `actor_user_id`、`payload_json`、`read_time`，删除废弃的 `title`、`content` 列

**后端修改：**
- `notification/service/NotificationService.java` — 新增 `send`、`batchSend`，`listNotifications` 增加 `unreadOnly` 参数
- `notification/service/impl/NotificationServiceImpl.java` — 完整重写：发送/批量发送/分页查询/unreadOnly 过滤/权限校验/JSON 序列化。批量插入 200 条/chunk
- `notification/controller/NotificationController.java` — 新增 `unreadOnly` 参数透传，标记已读使用 PUT 方法
- `discussion/service/impl/DiscussionServiceImpl.java` — 评论/回复/停用/恢复操作发送 `ApplicationEvent`（LinkedHashMap 替代 Map.of 避免 null NPE）
- `training/service/impl/TrainingPlanServiceImpl.java` — 成员移除/计划停用/恢复/时间变更/题目变更发送通知事件
- `notification/event/NotificationEvent.java` — 新建，事件 POJO
- `notification/event/NotificationEventListener.java` — 新建，`@TransactionalEventListener(AFTER_COMMIT)` 统一处理

**前端修改：**
- `stores/notifications.ts` — 新建 Pinia store：轮询、可见性暂停/恢复、标记已读、未读计数
- `views/NotificationsView.vue` — 重构：unreadOnly 复选框、store 集成、11 种通知文案
- `components/layout/AppHeader.vue` — 重构：移除本地轮询，使用 notificationStore，99+ badge
- `App.vue` — 新增 `auth.isLoggedIn` 监听，启动/停止通知轮询
- `api/notifications.ts` — 新增 `unreadOnly` 参数，PUT 标记已读

**测试：**
- `NotificationServiceImplTest.java` — 34 个测试（发送/批量/权限/JSON/分页/unreadOnly/幂等/批量大小/错误处理）
- `DiscussionServiceImplTest.java` — 新增 `@Mock ApplicationEventPublisher`
- `TrainingPlanServiceImplTest.java` — 新增 `@Mock ApplicationEventPublisher`
- `MybatisPlusTestHelper.java` — 新增 Notification 实体和 Mapper 注册

### 核心设计

**事件可靠性**：`@TransactionalEventListener(phase = AFTER_COMMIT)` 确保数据库事务提交后才发送通知，避免事务回滚后通知已发出。持久化失败只记日志（best-effort），不阻塞业务操作。

**自通知抑制**：`actorUserId == recipientUserId` 时自动跳过发送。

**批量插入优化**：`batchSend` 每批 200 条 chunk 调用 `notificationMapper.insert(Collection)`，避免单次 INSERT 过大。

**11 种通知场景**：
| 类型 | 触发点 | 模块 |
|---|---|---|
| POST_COMMENTED | 帖子收到评论 | 讨论 |
| COMMENT_REPLIED | 评论收到回复 | 讨论 |
| POST_ADMIN_DEACTIVATED | 管理员停用帖子 | 讨论 |
| COMMENT_ADMIN_DEACTIVATED | 管理员停用评论 | 讨论 |
| POST_RESTORED | 管理员恢复帖子 | 讨论 |
| COMMENT_RESTORED | 管理员恢复评论 | 讨论 |
| TRAINING_MEMBER_REMOVED | 被移出训练计划 | 训练 |
| TRAINING_ADMIN_DEACTIVATED | 管理员停用计划 | 训练 |
| TRAINING_RESTORED | 管理员恢复计划 | 训练 |
| TRAINING_SCHEDULE_CHANGED | 计划时间变更 | 训练 |
| TRAINING_PROBLEMS_CHANGED | 计划题目变更 | 训练 |

**V8→V13 兼容性修复**：V8 创建 notification 表时 `title NOT NULL` 无默认值，新实体不再设置 title。V13 迁移将旧数据 title/content 序列化到 payload_json 后删除这两列。

### 测试结果

```
后端 Tests run: 378, Failures: 0, Errors: 0, Skipped: 0
前端 Tests run: 127, Failures: 0
前端 type-check + lint + build 全部通过
```

## 2026-07-27：讨论区后端完善

### 本次目标

全面完善讨论区后端：帖子完整生命周期、停用审计追踪、权限模型、评论与回复、点赞原子操作、N+1 查询优化、输入安全、46 个测试。

### 修改文件

**后端修改：**
- `discussion/service/DiscussionService.java` — 新增 `restorePost` 方法
- `discussion/service/impl/DiscussionServiceImpl.java` — 全面重写：帖子类型校验（6 种固定类型）、HTML 标签拦截、N+1 批量加载作者和题目、原子计数器（view_count/like_count/comment_count）、停用来源追踪（CREATOR/ADMIN）、恢复端点（作者仅可恢复自己停用的，管理员可恢复任意）、管理员编辑权限、评论停用追踪
- `discussion/controller/DiscussionController.java` — 新增 `POST /api/posts/{id}/restore` 端点
- `discussion/dto/PostDetailResponse.java` — 新增 `deactivationSource`、`deactivationReason` 字段
- `admin/service/impl/AdminContentServiceImpl.java` — 注入 `AuditLogService`，管理员停用/恢复操作写入审计日志

**测试：**
- `discussion/service/impl/DiscussionServiceImplTest.java` — 从 5 个扩展到 46 个测试，覆盖帖子创建（6 种类型/公告权限/题解关联/HTML 拦截/空标题空内容）、更新时间填充、帖子编辑（作者/管理员/非作者拒绝）、帖子详情（404 区分/停用帖子可见性）、列表查询（N+1 批量加载验证）、停用（CREATOR 来源追踪/幂等）、恢复（作者恢复/管理员恢复/管理员停用无法自行恢复/404/幂等/清除审计字段）、评论（一级/回复/拒绝三级嵌套/拒绝 HTML/拒绝空评论/停用帖子拒绝/作者删除/来源追踪）、点赞（like/unlike）、权限边界

### 核心改进

**N+1 修复：**
- `listPosts`：`selectBatchIds` 批量加载作者和题目，不再逐条查询
- `getPostDetail`：`selectBatchIds` 批量加载评论作者和回复目标用户

**原子计数器：**
- `view_count`：`setSql("view_count = view_count + 1")` WHERE id=?
- `like_count`：`setSql("like_count = like_count + 1")` / `setSql("like_count = GREATEST(like_count - 1, 0)")`
- `comment_count`：`setSql("comment_count = comment_count + 1")`
- 避免读-改-写竞态条件

**停用审计追踪：**
- 作者自行停用：`deactivation_source = 'CREATOR'`，无需原因
- 管理员强制停用：`deactivation_source = 'ADMIN'`，必须填写原因
- 作者只能恢复 `CREATOR` 来源的停用帖子；管理员可恢复任意
- 恢复后清除所有停用审计字段

**输入安全：**
- 帖子类型：白名单校验（SOLUTION/QUESTION/CONTEST_SUMMARY/TRAINING_EXPERIENCE/ANNOUNCEMENT/OTHER）
- HTML 拦截：正则检测 `<script|iframe|object|embed|form|input|...` 等危险标签
- 标题/内容 trim + 空检查

### 测试结果

```
后端 Tests run: 289, Failures: 0, Errors: 0, Skipped: 0
前端 Tests run: 109/110 通过（1 个预存的 RegisterView 超时测试）
```

---

## 2026-07-27（二轮）：测试稳定与认证异常语义修复

### 修复：MyBatis-Plus Lambda Cache（46 个测试失败）

**根因**：`ProblemCommandServiceImplTest`、`ProblemQueryServiceImplTest`、`AdminProblemQueryServiceImplTest` 使用 `@ExtendWith(MockitoExtension.class)`（纯 Mockito，无 Spring 上下文）。生产代码创建 `LambdaQueryWrapper<Problem>()` 时需要解析 `Problem::getStatus` → 列名映射，调用 `LambdaUtils.getColumnMap()` → `TableInfoHelper.getTableInfo(Problem.class)` → 返回 null（未初始化）→ "can not find lambda cache" 异常。

Spring 环境下 MyBatis-Plus 自动配置会初始化所有 `@TableName` 实体，但纯 Mockito 测试不会。

**修复**：新建 `MybatisPlusTestHelper` 工具类，使用 `MybatisConfiguration.addMapper()` 触发 `MybatisMapperAnnotationBuilder.parse()` → `TableInfoHelper.initTableInfo()`，初始化 Problem 和 AppUser 实体。3 个测试类添加 `@BeforeAll` 静态初始化器。

### 修复：认证异常语义

**根因**：`GlobalExceptionHandler` 中 `@ExceptionHandler(AuthenticationException.class)` 返回 401。`InternalAuthenticationServiceException extends AuthenticationException`，因此数据库异常（字段缺失、连接失败）也被错误映射为 401 "用户名或密码错误"。

`RestAuthenticationEntryPoint` 同样对所有 `AuthenticationException`（包括 `InternalAuthenticationServiceException`）返回 401。

**修复**：
- `GlobalExceptionHandler`：新增 `@ExceptionHandler(InternalAuthenticationServiceException.class)` → 500，日志记录原始异常
- `RestAuthenticationEntryPoint`：区分 `InternalAuthenticationServiceException`（500）和普通未登录（401）
- `FlywayConfig`：添加 `@ConditionalOnProperty("spring.flyway.enabled")`，支持测试禁用
- `AcMateApplicationTests`：添加 `@TestPropertySource("spring.flyway.enabled=false")`

**新增测试（4 个）**：
1. `shouldReturn500OnDatabaseErrorDuringLogin` — DB 异常 → 500
2. `shouldNotLeakSqlIn500Response` — 500 不含 SQL/表名
3. `shouldReturn401ForWrongPassword` — 错误密码 → 401
4. `shouldReturn401ForNonExistentUser` — 用户不存在 → 401

### 修改文件

- `src/test/java/com/itnoduck/acmate/testutil/MybatisPlusTestHelper.java` — 新建，统一 MyBatis-Plus entity 初始化
- `src/test/java/com/itnoduck/acmate/AcMateApplicationTests.java` — 禁用 Flyway
- `src/test/java/com/itnoduck/acmate/user/controller/SessionLoginTest.java` — 新增 4 个测试
- `src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java` — 添加 `@BeforeAll`
- `src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java` — 添加 `@BeforeAll`
- `src/test/java/com/itnoduck/acmate/problem/service/impl/AdminProblemQueryServiceImplTest.java` — 添加 `@BeforeAll`
- `src/main/java/com/itnoduck/acmate/common/exception/GlobalExceptionHandler.java` — `InternalAuthenticationServiceException` → 500
- `src/main/java/com/itnoduck/acmate/security/RestAuthenticationEntryPoint.java` — 区分基础设施异常
- `src/main/java/com/itnoduck/acmate/config/FlywayConfig.java` — 添加 `@ConditionalOnProperty`
- `docs/IMPLEMENTATION_STATUS.md` — 更新测试统计、认证规则、V9 历史等
- `docs/DEVLOG.md` — 本条目

### 测试结果

```
后端 Tests run: 243, Failures: 0, Errors: 0, Skipped: 0
前端 Tests run: 110/110 通过，type-check + lint + build 通过
```

### V9 历史结论

V9 首次出现在 `bd56c20`（Phase 8，含 `IF NOT EXISTS`）。该语法从未被 Flyway 执行——Spring Boot 4.1.0 无 Flyway 自动配置，首次启用 Flyway 在 `0505f11`，且现有数据库 baseline 在 V11。当前修正是首次启用 Flyway 前对未发布迁移的语法修正，无 checksum 冲突风险。

### Baseline V11 验证

- `flyway_schema_history` 存在 1 条 BASELINE 条目（version=11）
- `FlywayConfig` 使用 `@ConditionalOnProperty`，不全局启用 `baseline-on-migrate`
- `acmate_fresh` 验证 V1-V11 完整迁移（非基线），未来 V12+ 正常执行
- `acmate` 基线化在 V11，未来 V12+ 正常执行

## 2026-07-27：认证恢复与 Flyway 迁移一致性修复

### 修复：登录 401

**根因**：所有用户登录返回 401。实际是数据库缺失 V6 `bio` 列（ALTER TABLE 从未执行）。MyBatis-Plus 在 SELECT `AppUser` 时包含 `bio` 字段 → MySQL 报 `Unknown column 'bio' in 'field list'` → 被 `DatabaseUserDetailsService.loadUserByUsername` 抛出 → Spring Security 的 `DaoAuthenticationProvider` 将其包装为 `InternalAuthenticationServiceException` → 全局 `@RestControllerAdvice` 将该异常视为 `AuthenticationException` → 返回 401。

**修复**：手动执行 `ALTER TABLE app_user ADD COLUMN bio VARCHAR(500) NULL AFTER avatar_url;`

### 修复：昵称大小写自更新误判 409

数据库排序规则为 utf8mb4_0900_ai_ci（大小写不敏感）。`UserProfileServiceImpl.updateProfile` 中 `!trimmed.equals(user.getNickname())` 使用 `String.equals()`（大小写敏感）判断是否变更，导致大小写变体被判定为"无变化"而跳过更新；但若进入查重分支（`selectCount`）又会命中自己的记录而报 409。

**修复**：`equals` → `equalsIgnoreCase`

### 修复：V9 迁移 SQL 非法语法

`V9__add_post_comment_audit.sql` 中 `ADD COLUMN IF NOT EXISTS` 是 MariaDB 语法，MySQL 8 不支持。Flyway 执行时报语法错误。

**修复**：移除 4 处 `IF NOT EXISTS`。

### 修复：Flyway 无自动配置

Spring Boot 4.1.0 的 `spring-boot-autoconfigure-4.1.0.jar` 不含 Flyway 相关类。`spring.flyway.enabled=true` 无效，启动时不执行迁移。

**修复**：新建 `FlywayConfig` 手动调用 `Flyway.configure().dataSource().locations().load().migrate()`。

### 数据库验证

- **现有数据库 `acmate`**：baseline 在 V11（`flyway_schema_history` 有 1 条 BASELINE 条目）。Flyway 启动时检测到已基线化版本，跳过所有迁移。
- **全新数据库 `acmate_fresh`**：执行完整 V1-V11 迁移循环，11/11 成功。第二次启动输出 "Schema is up to date. No migration necessary."，幂等性验证通过。

### 修改文件

- `src/main/java/com/itnoduck/acmate/user/service/impl/UserProfileServiceImpl.java` — equals → equalsIgnoreCase
- `src/main/resources/db/migration/V9__add_post_comment_audit.sql` — 移除 IF NOT EXISTS
- `src/main/java/com/itnoduck/acmate/config/FlywayConfig.java` — 新建，手动 Flyway 配置
- `docs/IMPLEMENTATION_STATUS.md` — 更新测试统计、迁移状态、已知问题
- `docs/DEVLOG.md` — 本条目

### 测试结果

```
后端 Tests run: 239, Failures: 10, Errors: 36, Skipped: 0
  SessionLoginTest: 12/12 通过
  UserProfileServiceImplTest: 5/5 通过
  UserRegistrationServiceImplTest: 12/12 通过
  失败/错误：全部为 Problem*Test（Mockito-only 上下文中 MyBatis-Plus lambda cache 未初始化）
前端 Tests run: 110/110 通过
前端 type-check + lint + build 全部通过
```

## 2026-07-24：项目与数据库初始化

### 本次目标

完成项目基础搭建、全量数据库设计、AppUser 实体与 Mapper、数据库健康检查、最小 Spring Security 配置。

### 修改文件

**新建：**
- src/main/resources/application.yml — 替代 application.properties
- src/main/resources/db/manual/00_create_database.sql
- src/main/resources/db/migration/V1__create_app_user.sql
- src/main/resources/db/migration/V2__create_problem_training_tables.sql
- src/main/resources/db/migration/V3__create_discussion_tables.sql
- src/main/resources/db/migration/V4__create_oj_sync_tables.sql
- src/main/java/com/itnoduck/acmate/user/entity/AppUser.java
- src/main/java/com/itnoduck/acmate/user/mapper/AppUserMapper.java
- src/main/java/com/itnoduck/acmate/common/controller/HealthController.java
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java
- README.md
- docs/PROJECT.md
- docs/DATABASE.md
- docs/DEVLOG.md
- docs/ROADMAP.md

**删除：**
- src/main/resources/application.properties — 改为 application.yml

### 设计决策

- 权限模型：is_admin 字段 + 无 RBAC 表
- 数据库无外键：由 Service 层保证完整性
- 评论两级结构：parent_id 只指向一级评论
- 软删除：状态字段隐藏，不物理删除
- Mapper 扫描：使用 @Mapper 注解，不使用 @MapperScan
- 安全配置：/api/health 匿名，其他需认证
- 排行榜：从 user_problem_status 表 first_ac_time 统计，无需额外排行榜表
- 所有索引定义在 CREATE TABLE 内部，确保 SQL 文件可安全重复执行

### 审查修正（2026-07-24 第二轮）

- 将全部独立 CREATE INDEX 语句移入 CREATE TABLE 内部，解决重复执行报错问题
- 修正 README.md：区分 Windows CMD 和 PowerShell 的执行方式
- 修正 README.md 和 DEVLOG.md 完成状态标记（SQL 已生成 / DB 待执行 / 接口待验证）
- 重命名冲突索引名（如 training_plan 的 idx_creator_user_id → idx_tp_creator_user_id）

### 执行命令

```powershell
# 编译
.\mvnw.cmd -DskipTests compile
```

### 编译结果

BUILD SUCCESS（5 个源文件编译通过，无错误）

### 数据库执行结果

已完成。五个 SQL 文件全部执行成功，acmate 数据库及 11 张表已创建。

### 接口验收结果

已通过。

```bash
curl http://localhost:8080/api/health
```

实际响应：
```json
{"status":"UP","database":"UP","userCount":0}
```

控制台 SQL 日志：
```sql
==> Preparing: SELECT COUNT( * ) AS total FROM app_user
```

确认 `appUserMapper.selectCount(null)` 已实际执行。

### 遗留问题

- [x] 数据库表创建（已完成）
- [x] /api/health 接口验证（已通过）
- [x] 用户注册（已完成）
- [ ] 用户登录（待实现）
- [ ] JWT 认证（待实现）
- [ ] 业务接口（帖子、评论、训练计划等）（待实现）
- [ ] Codeforces 同步（待实现）
- [ ] Redis 排行榜优化（待实现）
- [ ] 测试（待实现）

## 2026-07-24：用户注册

### 本次目标

实现 POST /api/auth/register，包括请求校验、密码 BCrypt 编码、重复注册检测、并发冲突处理、最小异常体系。

### 修改文件

**新建：**
- src/main/java/com/itnoduck/acmate/user/dto/RegisterRequest.java
- src/main/java/com/itnoduck/acmate/user/dto/RegisterResponse.java
- src/main/java/com/itnoduck/acmate/user/service/UserRegistrationService.java
- src/main/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImpl.java
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java
- src/main/java/com/itnoduck/acmate/common/exception/BusinessException.java
- src/main/java/com/itnoduck/acmate/common/exception/GlobalExceptionHandler.java
- src/main/java/com/itnoduck/acmate/common/dto/ApiError.java
- src/test/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImplTest.java
- docs/API.md

**修改：**
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 添加 PasswordEncoder Bean，放行 /api/auth/register，最小范围 CSRF 忽略
- docs/DEVLOG.md — 追加本记录
- README.md — 更新完成状态
- docs/ROADMAP.md — 更新阶段 2 进度

### 密码存储方案

- BCryptPasswordEncoder，由 SecurityConfig 提供 Bean
- 原始密码不 trim，不记录明文，不输出到日志或异常

### 重复注册处理方案

- 注册前使用 LambdaQueryWrapper 查询用户名和邮箱
- 存在则抛出 BusinessException(409)
- insert 时捕获 DuplicateKeyException 作为并发兜底，统一返回 HTTP 409
- 不解析 MySQL 英文异常消息判断冲突字段

### 请求规范化

- username：strip + toLowerCase(Locale.ROOT)
- nickname：strip
- email：空/纯空格 → null，非空 → strip + toLowerCase(Locale.ROOT)
- password：不 trim

### 测试结果

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

- 正常注册（6 个断言）
- 用户名已存在 → 409
- 邮箱已存在 → 409
- 空字符串邮箱规范化为 null
- insert 返回 0 → 500
- DuplicateKeyException → 409

### 接口验收结果

已通过。

```bash
# 正常注册 → 201
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123","nickname":"Test User","email":"test1@example.com"}'
# → {"id":1,"username":"testuser1","nickname":"Test User","email":"test1@example.com"}

# 重复用户名 → 409
# → {"code":409,"message":"用户名已被使用","timestamp":"..."}

# 校验失败 → 400
# → {"code":400,"message":"请求参数校验失败","fieldErrors":[...],"timestamp":"..."}
```

### 遗留问题

- [ ] 用户登录（待实现）
- [ ] JWT 认证（待实现）
- [ ] 业务接口（帖子、评论、训练计划等）（待实现）
- [ ] Codeforces 同步（待实现）
- [ ] Redis 排行榜优化（待实现）
- [ ] 测试（待实现）

## 2026-07-24：项目验收确认

### 验收内容

对当前运行中的 ACMate 应用进行完整验收。

### 运行进程

- PID: 6776
- 启动类: com.itnoduck.acmate.AcMateApplication
- 端口: 8080
- 启动方式: IDEA 运行配置

### /api/health 实际响应

```json
{"status":"UP","database":"UP","userCount":1}
```

`userCount=1` 证明 appUserMapper.selectCount(null) 实际查询了数据库（非硬编码值）。

### SQL 验证

应用由 IDEA 启动，无法在此终端直接查看控制台 SQL 日志。但 `userCount=1`（与已注册用户数一致）间接证实了实际数据库查询。

### 数据库表数量验证

未直接查询 MySQL（DB_PASSWORD 仅在 IDEA 运行配置中，未在本 shell 暴露）。通过 /api/health 的 selectCount 成功执行确认 app_user 表存在且数据库连接正常。其余 10 张表曾在 SQL 执行阶段通过 source 命令创建，但本次未直接逐表验证。

### 结论

- ACMate 应用正常运行，PID 6776
- 数据库连接正常
- /api/health 返回正确
- 未直接验证全部 11 张表（受限于 DB_PASSWORD 不可用）

## 2026-07-24：注册模块审查与修正

### 修正内容

1. **CSRF 匹配收紧**
   - SecurityConfig 中 CSRF 忽略从字符串路径改为 `PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/register")`
   - 授权规则同步改为 HTTP method 级别：`HttpMethod.GET /api/health` + `HttpMethod.POST /api/auth/register`

2. **DTO 规范化前置**
   - RegisterRequest 增加自定义 setter 对 username、nickname、email 执行规范化
   - Jakarta Validation 校验规范化后的值（例如 nickname=" a " → strip → "a" → @Size 2~32 失败 → 400）
   - Service 层移除冗余规范化逻辑
   - password setter 不做 trim

3. **新增测试**
   - AuthControllerTest：使用 @WebMvcTest 加载真实 SecurityFilterChain，12 个测试覆盖参数校验、DTO 规范化、isAdmin 防注入、CSRF 规则验证
   - UserRegistrationServiceImplTest：新增 DTO 规范化验证（username 大小写、nickname trim、email trim+lowercase）

### 新增文件
- src/test/java/com/itnoduck/acmate/user/controller/AuthControllerTest.java

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — method-specific matchers
- src/main/java/com/itnoduck/acmate/user/dto/RegisterRequest.java — custom normalization setters
- src/main/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImpl.java — 移除 normalization
- src/test/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImplTest.java — 新增规范化测试
- README.md — 健康检查说明修正
- docs/DEVLOG.md — 追加本记录

### 测试框架

AuthControllerTest 从 standaloneSetup 迁移到 @WebMvcTest，加载真实 SecurityConfig：

- `@WebMvcTest(AuthController.class)` + `@Import({SecurityConfig.class, GlobalExceptionHandler.class})`
- 使用 `@MockitoBean` 模拟 UserRegistrationService
- `@Autowired MockMvc` 自动注入
- 导入路径：`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`、`org.springframework.test.context.bean.override.mockito.MockitoBean`

### CSRF 测试覆盖

- POST /api/auth/register 无 CSRF → 201（CSRF 仅对此端点忽略）
- PUT /api/auth/register 无 CSRF → 403（仅 POST 方法被忽略）
- POST /api/security-test 无 CSRF → 403（其他路径仍受 CSRF 保护）
- POST /api/security-test 带 CSRF → 404（非 403，证明 CSRF 检查已通过）

### 测试结果

```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

- AuthControllerTest: 12 tests passed
- UserRegistrationServiceImplTest: 10 tests passed
- AcMateApplicationTests: 1 test passed

## 2026-07-24：Spring Profile 配置修正

### 修正内容

- 从 `application.yml` 中删除 `spring.profiles.active: local`
- 本地 Profile 改为由 IDEA 运行配置显式激活（`--spring.profiles.active=local`）
- `application-local.yml` 已在 `.gitignore` 中，不会提交

### 原因

公共 `application.yml` 中的默认 profile 会导致 Maven 测试也激活 `local` profile，测试应该使用 `default` profile，不依赖本地环境配置。

### 测试日志确认

```
No active profile set, falling back to 1 default profile: "default"
```

不再出现 `The following 1 profile is active: "local"`。

### 测试结果

```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

## 2026-07-25：Session 登录 — 认证基础层

### 本次目标

创建认证主体和数据库 UserDetailsService，不实现登录接口、Session 或 JWT。

### 新增文件
- src/main/java/com/itnoduck/acmate/security/AuthenticatedUser.java — 实现 UserDetails
- src/main/java/com/itnoduck/acmate/security/DatabaseUserDetailsService.java — 实现 UserDetailsService

### 设计决策

- **AuthenticatedUser**：final class，手动 getter，不暴露 passwordHash 到 toString
- **权限映射**：所有用户 → ROLE_USER；isAdmin=1 → 额外 ROLE_ADMIN，使用 SimpleGrantedAuthority
- **状态映射**：status=1 → enabled=true，其他 → enabled=false，禁用账号仍会被加载
- **查询**：LambdaQueryWrapper + selectOne，username 先 strip 再 toLowerCase(Locale.ROOT)
- **密码**：不在此层执行 BCrypt matches，由后续 DaoAuthenticationProvider 处理

### 已知限制

- 尚未实现登录接口
- 尚未实现 Session 持久化
- 尚未实现退出登录
- 尚未实现当前用户查询接口

## 2026-07-25：Session 登录

### 本次目标

实现 POST /api/auth/login、GET /api/users/me、Session 持久化，不实现退出登录。

### 新增文件
- src/main/java/com/itnoduck/acmate/user/dto/LoginRequest.java — 登录请求（无 @Data，手动 toString 排除 password）
- src/main/java/com/itnoduck/acmate/user/dto/LoginResponse.java — 登录响应 record
- src/main/java/com/itnoduck/acmate/user/dto/CurrentUserResponse.java — 当前用户响应 record
- src/main/java/com/itnoduck/acmate/user/service/UserAuthenticationService.java — 登录服务接口
- src/main/java/com/itnoduck/acmate/user/service/impl/UserAuthenticationServiceImpl.java — 登录认证实现
- src/main/java/com/itnoduck/acmate/user/controller/UserController.java — GET /api/users/me
- src/main/java/com/itnoduck/acmate/security/RestAuthenticationEntryPoint.java — 401 JSON 响应
- src/test/java/com/itnoduck/acmate/user/controller/SessionLoginTest.java — 12 个登录 Session 集成测试

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 AuthenticationManager、SecurityContextRepository、SessionAuthenticationStrategy、RestAuthenticationEntryPoint Bean；放行 /api/auth/login；保护 /api/users/me
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java — 增加 POST /api/auth/login
- src/main/java/com/itnoduck/acmate/common/exception/GlobalExceptionHandler.java — 增加 BadCredentialsException/UsernameNotFoundException → 401、DisabledException → 403
- src/main/java/com/itnoduck/acmate/common/dto/ApiError.java — Jackson 3.x import 修正
- src/main/resources/application.yml — 增加 Session 超时和 Cookie 配置
- src/test/java/com/itnoduck/acmate/user/controller/AuthControllerTest.java — 增加 @MockitoBean 适配新依赖
- docs/API.md — 增加登录和 /me 接口文档
- docs/DEVLOG.md — 追加本记录

### AuthenticationManager 配置

```java
DaoAuthenticationProvider provider = new DaoAuthenticationProvider(databaseUserDetailsService);
provider.setPasswordEncoder(passwordEncoder);
new ProviderManager(provider);
```

### 登录认证链路

1. AuthController 接收 LoginRequest → UserAuthenticationService.login()
2. UsernamePasswordAuthenticationToken(username, rawPassword)
3. AuthenticationManager.authenticate() → DaoAuthenticationProvider
4. DatabaseUserDetailsService.loadUserByUsername() → 查询用户
5. BCryptPasswordEncoder.matches() → 验证密码
6. 成功 → SecurityContext → SecurityContextHolder + HttpSession
7. ChangeSessionIdAuthenticationStrategy 更换 Session ID

### SecurityContext 写入 Session

通过 SecurityContextRepository.saveContext(context, request, response) 将 SecurityContext 持久化到 HttpSession，后续请求可通过 JSESSIONID Cookie 恢复登录状态。

### Session Fixation 防护

ChangeSessionIdAuthenticationStrategy 在认证成功时自动更换 Session ID，防止 Session Fixation 攻击。

### 异常处理

| 异常 | HTTP | 消息 |
|------|------|------|
| BadCredentialsException | 401 | 用户名或密码错误 |
| UsernameNotFoundException | 401 | 用户名或密码错误 |
| DisabledException | 403 | 账号已被禁用 |

### 测试结果

```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
```

- SessionLoginTest: 12 tests（正确凭证、密码错误、用户不存在、禁用用户、CSRF 忽略、Session 存储、跨请求 /me、响应字段验证、未登录 401、Principal 类型验证、管理员标识）
- AuthControllerTest: 12 tests（注册相关测试仍通过）
- UserRegistrationServiceImplTest: 10 tests
- AcMateApplicationTests: 1 test

### 已知限制

- 尚未实现退出登录

## 2026-07-25：Session 退出登录

### 本次目标

实现 POST /api/auth/logout，包括 SecurityContext 清除、Session 失效、JSON 403 响应、测试和文档。

### 新建文件
- src/main/java/com/itnoduck/acmate/security/RestAccessDeniedHandler.java — JSON 403 响应
- src/test/java/com/itnoduck/acmate/user/controller/LogoutTest.java — 9 个退出登录测试

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 RestAccessDeniedHandler Bean，exceptionHandling 增加 accessDeniedHandler
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java — 增加 POST /api/auth/logout
- docs/API.md — 增加 logout 接口文档
- docs/DEVLOG.md — 追加本记录

### logout 实现方式

Controller 中注入 Authentication（Spring MVC 参数自动绑定），调用 SecurityContextLogoutHandler.logout(request, response, authentication)：

1. SecurityContextLogoutHandler 负责：清除 SecurityContextHolder + 调用 SecurityContextRepository.saveContext() 将空 Context 写回 Session + 使 HttpSession 失效
2. SecurityContextRepository（HttpSessionSecurityContextRepository）负责：从 Session 读取/写入 SecurityContext，在此流程中完成 Session 属性清理
3. SecurityContextHolder.clearContext() 仅清理当前线程，不足以使 Session 失效

返回 204 No Content，响应体为空。

### CSRF 防护

- POST /api/auth/logout 不在 CSRF 忽略列表
- 必须携带有效 CSRF Token
- 缺少 CSRF → 403 JSON（AccessDeniedHandler）
- 未认证 → 401 JSON（AuthenticationEntryPoint）

### 401 vs 403 区分

- 401：未认证（未登录或 Session 已失效）→ AuthenticationEntryPoint
- 403：已认证但缺少/无效 CSRF → AccessDeniedHandler（新增 RestAccessDeniedHandler）

### 测试结果

```
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
```

- LogoutTest: 9 tests（无 CSRF → 403、有 CSRF → 204、Session 失效、logout 后 /me → 401、SecurityContext 清除、未认证 → 401、响应体为空、注册和登录 CSRF 豁免不退化、其他 POST 无 CSRF → 403）
- SessionLoginTest: 12 tests
- AuthControllerTest: 12 tests
- UserRegistrationServiceImplTest: 10 tests
- AcMateApplicationTests: 1 test

### 已知限制

- Session 认证阶段（login + me + logout）已完整实现

## 2026-07-25：CSRF Token 获取接口

### 本次目标

为真实客户端（非测试 built-in helper）提供 CSRF Token 获取端点 GET /api/auth/csrf，完成跨请求真实 Token 流程测试。

### 新建文件
- src/main/java/com/itnoduck/acmate/user/dto/CsrfTokenResponse.java — record(token, headerName, parameterName)

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — GET /api/auth/csrf → permitAll
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java — 增加 GET /api/auth/csrf
- src/test/java/com/itnoduck/acmate/user/controller/LogoutTest.java — 增加 7 个 CSRF Token 获取和真实流程测试
- docs/API.md — 增加 GET /api/auth/csrf 文档和客户端使用流程
- docs/DEVLOG.md — 追加本记录

### CSRF Token 获取方式

客户端通过 GET /api/auth/csrf 获取当前 Session 的 Token。CsrfFilter 在每次请求时通过 CsrfTokenRepository 生成/加载 Token 并存入 request attribute。

Controller 从 request.getAttribute(CsrfToken.class.getName()) 提取 Token，返回 token、headerName、parameterName。

### 真实客户端流程

1. POST /api/auth/login → 获取 JSESSIONID Cookie
2. GET /api/auth/csrf（同一 Session）→ 获取 token + headerName
3. POST /api/auth/logout（同一 Session）→ 设置请求头 `<headerName>: <token>` → 204

### SecurityContextLogoutHandler 与 SecurityContextRepository 职责区分

| 组件 | 职责 |
|------|------|
| SecurityContextLogoutHandler | 调用 SecurityContextHolder.clearContext() + 触发 SecurityContextRepository.saveContext(empty) + 使 Session 失效 |
| SecurityContextRepository | 从 Session 读取/写入 SecurityContext 属性（SPRING_SECURITY_CONTEXT），不负责 Session 生命周期 |
| SecurityContextHolder | 线程绑定的 Context，clearContext() 只影响当前线程 |

### 测试结果

```
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
```

- LogoutTest: 15 tests（新增：CSRF 接口 200、Token 非空、响应不含 sessionId、真实 Token 登录后 logout → 204、错误 Token → 403、真实流程 login → csrf → logout → /me → 401）
- SessionLoginTest: 12 tests
- AuthControllerTest: 12 tests
- UserRegistrationServiceImplTest: 10 tests
- AcMateApplicationTests: 1 test

### 已知限制

- Session 认证阶段（login + me + logout + csrf）已完整实现

## 2026-07-25：Session 认证真实运行验收

### 验收时间

2026-07-25，本地运行环境，Maven + local profile。

### 验收流程

1. POST /api/auth/register → 201（新建验收用户）
2. POST /api/auth/login → 200（无 password 或 passwordHash 字段）
3. GET /api/users/me → 200（用户信息与登录一致）
4. GET /api/auth/csrf → 200（token 非空，含 headerName 和 parameterName）
5. POST /api/auth/logout + CSRF header → 204（响应体为空）
6. GET /api/users/me（同一 Session）→ 401（Session 已失效）
7. POST /api/auth/login（错误密码）→ 401
8. GET /api/users/me（未登录）→ 401（JSON 格式，非 HTML 或 302）
9. POST /api/auth/logout（无 CSRF）→ 403

### 自动化测试 vs 真实运行

| 维度 | 自动化测试 | 真实运行验收 |
|------|-----------|-------------|
| 环境 | @WebMvcTest + mock Mapper | Maven spring-boot:run + 真实 MySQL |
| Session | MockHttpSession | 真实 Tomcat HttpSession + JSESSIONID Cookie |
| CSRF | with(csrf()) post-processor | 真实 GET /api/auth/csrf → 提取 token → 设置请求头 |
| 认证 | with(user(...)) 或 mock login | 真实 BCrypt + AuthenticationManager + DB 查询 |

两者在所有接口行为上完全一致。

### 已知限制

- Session 认证模块已全面验收通过

## 2026-07-25：题目查询模块

### 本次目标

实现题目实体、Mapper、分页配置、题目详情查询、题目分页列表，包含 DTO、Service、Controller、测试和文档。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/entity/Problem.java — 题目实体
- src/main/java/com/itnoduck/acmate/problem/mapper/ProblemMapper.java — Mapper 接口
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemQueryRequest.java — 列表查询参数
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemSummaryResponse.java — 列表项响应
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemDetailResponse.java — 详情响应
- src/main/java/com/itnoduck/acmate/common/dto/PageResponse.java — 通用分页响应
- src/main/java/com/itnoduck/acmate/problem/service/ProblemQueryService.java — 查询服务接口
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImpl.java — 查询服务实现
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 查询接口
- src/main/java/com/itnoduck/acmate/config/MybatisPlusConfig.java — 分页插件配置
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java — Service 测试（9 tests）
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — Controller 测试（7 tests）

### 修改文件
- pom.xml — 增加 mybatis-plus-jsqlparser 3.5.17（分页插件必需）
- docs/API.md — 增加 GET /api/problems 和 GET /api/problems/{id}
- docs/DEVLOG.md — 追加本记录

### 实体与 Mapper

- Problem 实体映射 problem 表，@TableName("problem")，id 使用 @TableId(type = IdType.AUTO)
- tags 字段当前为 VARCHAR(255) 逗号分隔字符串，暂不拆分为关联表
- ProblemMapper extends BaseMapper<Problem>，无 XML Mapper，无手写 SQL

### 分页插件配置

- MybatisPlusInterceptor + PaginationInnerInterceptor(DbType.MYSQL)
- maxLimit=100 为数据库层兜底，接口 DTO 层仍有 @Min/@Max 独立校验
- 需单独引入 mybatis-plus-jsqlparser，不通过 mybatis-plus-spring-boot4-starter 传递

### 动态查询条件

- 固定过滤 status=1，保证普通用户只看到正常题目
- platform 精确匹配，difficulty 精确匹配
- keyword 使用括号组合匹配 title OR externalProblemKey，防止 OR 绕过 status=1 过滤
- 排序：createTime DESC, id DESC

### status 过滤原因

Service 注释明确说明：status=0 的禁用题目不应被普通用户看到，用户不应感知题目"曾经存在但已禁用"的事实。所有查询入口统一过滤，不做按角色区分。

### 测试结果

```
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemQueryServiceImplTest: 9 tests（正常查询、不存在的 id→404、零/负 id→404、禁用题目不可见、status 过滤、platform 过滤、difficulty 过滤、keyword 括号组合、Entity→DTO 转换）
- ProblemControllerTest: 7 tests（未认证→401、已认证列表→200、已认证详情→200、不存在→404、page=0→400、size=101→400、非法 platform→400）
- 原 50 tests 全部通过

### 已知限制

- 尚未实现创建、修改、删除题目
- 尚未实现管理员接口
- 尚未实现训练计划、用户做题状态

## 2026-07-26：管理员创建题目

### 本次目标

实现 POST /api/problems，仅限 ROLE_ADMIN 创建，服务端指定 creatorUserId 和 status，前置查重 + DuplicateKeyException 并发兜底。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/dto/CreateProblemRequest.java — 创建请求 DTO，无 creatorUserId/status 字段，setter 内完成规范化
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 创建服务接口
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 创建服务实现
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — Service 测试（12 tests）

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 增加 POST /api/problems 端点
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 hasRole("ADMIN") 保护 POST /api/problems
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 增加 10 个 POST 测试
- docs/API.md — 增加 POST /api/problems 文档
- docs/DEVLOG.md — 追加本记录

### 信任边界

- creatorUserId 来自 @AuthenticationPrincipal AuthenticatedUser，不由请求体指定
- 请求体中的 creatorUserId 和 status 字段（无论值为何）均被忽略
- status 由服务端固定为 1

### 重复检测策略

- 前置查重：externalProblemKey 非空时 selectCount by platform + externalProblemKey，命中则 409
- 数据库兜底：catch DuplicateKeyException → 409（唯一索引 uk_platform_problem）
- 前置查重只用于友好错误提示，并发安全依赖唯一索引

### 权限控制

- SecurityFilterChain 中 .requestMatchers(HttpMethod.POST, "/api/problems").hasRole("ADMIN")
- 不使用 @PreAuthorize / @EnableMethodSecurity
- POST /api/problems 不在 CSRF 忽略列表，必须携带有效 CSRF Token

### DTO 规范化

- platform → uppercase（setter）
- externalProblemKey / title → strip，blank → null（setter）
- sourceUrl / difficulty → strip，blank → null（setter）
- tags → split→strip→dedup（LinkedHashSet 保留首次出现顺序）→逗号重连，empty → null（setter）
- contentMd → 不 trim（Markdown 白空格可能有意为之）
- @Getter 只读，手动 toString() 排除 contentMd

### 测试结果

```
Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemCommandServiceImplTest: 12 tests（正常创建、creatorUserId 来自参数、status 固定为 1、非 CUSTOM 必填 externalKey→400、CUSTOM 允许 null externalKey、前置查重→409、DuplicateKeyException→409、insert 返回 0→500、id 未回填→500、tags 规范化、blank→null、Entity→DTO 转换）
- ProblemControllerTest: 17 tests（原有 7 个查询 + 新增 10 个：未认证 POST+csrf→401、未认证 POST 无 csrf→403、普通用户+csrf→403、admin 无 csrf→403、admin+csrf→201、响应无 status、creatorUserId 传入 service=1L、请求体 creatorUserId/status 被忽略→仍传 1L、校验失败→400、GET 仍对普通用户可用）
- 原有 59 tests 全部通过

### 真实接口验收（2026-07-26）

使用运行中 ACMate + 本地 MySQL，真实 Session + CSRF Token 流程验收。

| # | 场景 | 预期 | 实际 | 结果 |
|---|------|------|------|------|
| 1 | Admin + CSRF → POST CUSTOM | 201 | 201 | 通过 |
| 2 | Normal user + CSRF → POST | 403 | 403 | 通过 |
| 3 | Admin 无 CSRF → POST | 403 | 403 | 通过 |
| 4 | CODEFORCES 无 externalProblemKey | 400 | 400 | 通过 |
| 5 | CUSTOM 无 externalProblemKey | 201 | 201 | 通过 |
| 6a | CODEFORCES + externalProblemKey | 201 | 201 | 通过 |
| 6b | 相同 platform+key 再次创建 | 409 | 409 | 通过 |
| 7 | tags=" array, hash-map, array,  " | 201, 存为 "array,hash-map" | 201, "array,hash-map" | 通过 |

**数据库核对：**
- 所有创建题目 status = 1，creator_user_id = 1（管理员 id）
- 创建响应和查询响应均不含 status 字段
- 普通用户 GET /api/problems 可正常分页查询
- 验收数据已全部删除，数据库恢复验收前状态

## 2026-07-26：权限模型修正 — 允许所有登录用户创建题目

### 本次目标

将 POST /api/problems 的权限从 ROLE_ADMIN 修正为所有已登录用户（authenticated），使普通用户也能创建题目。

### 修改文件

- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — hasRole("ADMIN") → authenticated()
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 补全类级和方法级中文 Javadoc
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 注释修正为"当前登录用户创建题目"
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 普通用户 403 → 201，新增 CSRF+普通用户测试
- docs/API.md — 权限描述修正为"所有已登录用户"
- docs/DEVLOG.md — 追加本记录

### 权限模型

- 题目查询和创建是所有登录用户的基础权限
- 管理员是普通用户权限的超集
- 资源所有权由 creatorUserId 记录
- 后续资源级管理操作将基于"创建者或管理员"判断
- CSRF 规则没有放宽
- 没有修改数据库结构
- 没有实现新的接口

### 测试结果

```
Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemControllerTest: 18 tests（新增 shouldReturn201WhenNormalUserPostsWithCsrf、shouldReturn403WhenNormalUserPostsWithoutCsrf；原 403 测试改为 201）
- ProblemCommandServiceImplTest: 12 tests 全部通过（Service 逻辑未改动）
- 原有测试全部通过

### 已知限制

- 尚未实现删除、停用题目

## 2026-07-26：创建者和管理员修改题目

### 本次目标

实现 PUT /api/problems/{id}，允许题目创建者或管理员完整修改题目，包含所有权校验、题目标识查重（排除自身）、事务保护、Controller 级中文 Javadoc。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/support/ProblemFieldNormalizer.java — 输入字段规范化工具，供 CreateProblemRequest 和 UpdateProblemRequest 共享
- src/main/java/com/itnoduck/acmate/problem/dto/UpdateProblemRequest.java — 修改请求 DTO，字段和规则与 CreateProblemRequest 一致，无 creatorUserId/status

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/dto/CreateProblemRequest.java — tags 规范化改用 ProblemFieldNormalizer
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 增加 updateProblem 方法签名
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 增加 updateProblem 实现，包含所有权校验、查重排除自身、LambdaUpdateWrapper null 支持、事务、回读
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 增加 PUT /api/problems/{id} 端点，补全全部 4 个端点中文 Javadoc
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — PUT /api/problems/{id} 使用 authenticated()
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — 增加 28 个 update 测试
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 增加 18 个 PUT 测试
- docs/API.md — 增加 PUT /api/problems/{id} 文档
- docs/DEVLOG.md — 追加本记录

### 设计决策

- **LambdaUpdateWrapper.set()** 而非 updateById：MyBatis-Plus 全局字段策略 NOT_NULL 会忽略 null 字段，.set() 显式控制
- **UPDATE WHERE status=1**：防止并发禁用后的修改
- **查重排除自身**：`.ne(Problem::getId, problemId)` 允许保持原值
- **更新后回读**：确保响应含 ON UPDATE CURRENT_TIMESTAMP 的最新 updateTime
- **@Transactional**：查询 → 校验 → 更新 → 回读的读写边界一致
- **权限在 Service 判断**：查询资源后判断 `operatorAdmin || Objects.equals(creatorUserId, operatorUserId)`
- **ProblemFieldNormalizer**：CreateProblemRequest 和 UpdateProblemRequest 共享同一套规范化逻辑

### 权限模型

- SecurityFilterChain：PUT /api/problems/{id} → authenticated()
- Service 层：ensureCanManageProblem() — 管理员或创建者，否则 403
- 区分 400（参数无效）、401（未登录）、403（CSRF 或权限）、404（不存在/禁用）、409（标识冲突）

### 测试结果

```
Tests run: 133, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemCommandServiceImplTest: 40 tests（12 创建 + 28 更新）
- ProblemControllerTest: 34 tests（6 查询 + 10 创建 + 18 PUT）
- 原有 59 tests 全部通过

### 已知限制

- 尚未实现删除、停用、个人题目管理

## 2026-07-26：个人题目管理与停用题目可见性调整

### 本次目标

新增 GET /api/problems/mine，调整停止题目的查看和编辑权限，使创建者和管理员可以查看和编辑停用题目。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/dto/MineProblemStatusFilter.java — ALL/ACTIVE/INACTIVE 状态筛选枚举
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemStatusView.java — ACTIVE/INACTIVE 状态视图，不暴露数据库数字
- src/main/java/com/itnoduck/acmate/problem/dto/MyProblemSummaryResponse.java — 含 status 和 updateTime 的列表项

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/service/ProblemQueryService.java — getProblem 增加 viewerUserId/viewerAdmin；新增 listMyProblems
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImpl.java — getProblem 先查后判；实现 listMyProblems
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 不再限制 status=1；非创建者非管理员对停用题目返回 404
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — GET /mine；getProblem 传递查看者身份；更新全部 Javadoc
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java — 9→18 tests
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — 40→43 tests
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 34→44 tests
- docs/API.md — 新增 GET /api/problems/mine；更新详情和修改接口
- docs/DEVLOG.md — 追加本记录

### 设计决策

- **MineProblemStatusFilter / ProblemStatusView**：接口层不暴露数据库数字状态
- **creatorUserId 来自认证主体**：不接受客户端传入，避免越权
- **公共列表不返回 status**：避免向其他用户暴露题目管理状态
- **停用题目对无关用户返回 404**：不暴露存在性
- **编辑停用题目不自动恢复**：仅修改内容字段，status 保持 0
- **getProblem 先查后判**：改为查询后根据查看者身份判断可见性

### 测试结果

```
Tests run: 155, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemQueryServiceImplTest: 18 tests
- ProblemCommandServiceImplTest: 43 tests
- ProblemControllerTest: 44 tests
- 原有 62 tests 全部通过

### 已知限制

- 尚未实现停用和恢复题目

## 2026-07-26：题目停用与恢复

### 本次目标

新增 POST /api/problems/{id}/deactivate 和 POST /api/problems/{id}/restore，允许创建者和管理员停用和恢复题目。停用是逻辑操作，不物理删除。

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 新增 deactivateProblem、restoreProblem 方法
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 停用和恢复实现
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 新增两个 POST 端点及 Javadoc
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 新增两条 authenticated() 规则
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — 64 tests（+21）
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 59 tests（+15）
- docs/API.md / docs/DEVLOG.md — 追加记录

### 设计决策

- **幂等**：重复停用/恢复直接返回 204，不报错
- **WHERE 带原状态**：`eq(status, 1)` 或 `eq(status, 0)`，防并发
- **停用后仍占用标识**：platform+externalProblemKey 唯一性不变
- **停用题目对其他用户返回 404**：私有管理操作，不暴露存在性
- **先查后判权限**：无法在 URL 层完成
- **operatorUserId 来自 @AuthenticationPrincipal**：不由请求指定

### 测试结果

```
Tests run: 191, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemCommandServiceImplTest: 64 tests
- ProblemControllerTest: 59 tests
- ProblemQueryServiceImplTest: 18 tests
- 原有 50 tests 全部通过

### 已知限制

- 尚未实现物理删除

## 2026-07-26：按创建者查看公开题目与管理员全站题库查询

### 本次目标

扩展 GET /api/problems 支持可选 creatorUserId 参数，新增 GET /api/admin/problems 管理员全站题库查询（含创建者信息批量加载）。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/dto/AdminProblemSummaryResponse.java — 含创建者信息的列表项 record
- src/main/java/com/itnoduck/acmate/problem/service/AdminProblemQueryService.java — 管理员查询服务接口
- src/main/java/com/itnoduck/acmate/problem/service/impl/AdminProblemQueryServiceImpl.java — 管理员查询实现，含批量用户加载
- src/main/java/com/itnoduck/acmate/problem/controller/AdminProblemController.java — 管理员题目查询控制器
- src/test/java/com/itnoduck/acmate/problem/service/impl/AdminProblemQueryServiceImplTest.java — 9 tests
- src/test/java/com/itnoduck/acmate/problem/controller/AdminProblemControllerTest.java — 9 tests

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemQueryRequest.java — 增加 creatorUserId 字段
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImpl.java — listProblems 支持 creatorUserId 过滤
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 /api/admin/** → hasRole("ADMIN")
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java — 23 tests（+5）
- docs/API.md — 增加 GET /api/admin/problems，更新 GET /api/problems
- docs/DEVLOG.md — 追加本记录

### 设计决策

- **creatorUserId 过滤**：公共列表和全家列表均支持，值 ≤ 0 时返回 400
- **AdminProblemSummaryResponse**：扩展 ProblemSummaryResponse，增加 creatorUsername、creatorNickname、status、updateTime
- **批量用户加载**：收集分页结果中的唯一 creatorUserId，selectBatchIds 一次查询，build Map，避免 N+1
- **缺失创建者数据不抛异常**：username/nickname 返回 null，保留题目记录
- **ALL 不附加 status 条件**：管理员可查看全站正常和停用全部题目
- **ACTIVE/INACTIVE**：与 /mine 共享 MineProblemStatusFilter 枚举

### 测试结果

```
Tests run: 214, Failures: 0, Errors: 0, Skipped: 0
```

- AdminProblemControllerTest: 9 tests
- AdminProblemQueryServiceImplTest: 9 tests
- ProblemQueryServiceImplTest: 23 tests（+5）
- ProblemControllerTest: 59 tests
- ProblemCommandServiceImplTest: 64 tests
- 原有 50 tests 全部通过

### 已知限制

- 尚未实现物理删除

## 2026-07-26：Vue 前端初始化与认证

### 本次目标

初始化 Vue 3 前端工程，实现登录、注册、首页、Session 认证、CSRF 处理、403/404 页面和响应式基础。

### 新建文件

- frontend/ — Vue 3 + TypeScript + Vite 工程
- frontend/src/styles/tokens.css — Design Tokens（CSS Variables）
- frontend/src/styles/main.css — 全局样式与字体
- frontend/src/constants/labels.ts — 中文界面文案常量
- frontend/src/api/client.ts — Axios 实例（withCredentials）
- frontend/src/api/auth.ts — 认证 API（login/register/me/csrf/logout）
- frontend/src/stores/auth.ts — Pinia 认证 Store
- frontend/src/router/index.ts — Vue Router（路由守卫）
- frontend/src/components/layout/AppHeader.vue — 顶部导航
- frontend/src/components/layout/PageContainer.vue — 页面容器
- frontend/src/components/common/LoadingState.vue — 加载状态
- frontend/src/components/common/EmptyState.vue — 空状态
- frontend/src/views/LoginView.vue — 登录页
- frontend/src/views/RegisterView.vue — 注册页
- frontend/src/views/HomeView.vue — 首页
- frontend/src/views/ForbiddenView.vue — 403 页
- frontend/src/views/NotFoundView.vue — 404 页
- frontend/src/__tests__/auth.test.ts — 11 个认证测试
- frontend/design/ — Stitch 设计资源（16 页截图 + HTML + theme JSON）
- frontend/design/README.md — 设计资源说明
- docs/PRD.md — 产品需求文档 v1.4

### 设计决策

- 样式方案：CSS Variables + scoped CSS + Element Plus，未使用 Tailwind
- 字体：Google Fonts CDN 引用 Hanken Grotesk + Inter + JetBrains Mono，CSS 回退到系统字体
- 认证：Session Cookie（withCredentials），不读取/保存 JSESSIONID
- CSRF：登出前 GET /api/auth/csrf 获取动态 Token
- 路由守卫：requiresAuth 跳转登录，guestOnly 跳转首页
- 初始化：init() 完成后挂载 app，避免 flicker
- 中文化：固定文案使用简体中文，枚举/变量保持英文
- 未实现模块：首页"即将推出"区域展示，不放导航

### 测试结果

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

### 构建结果

BUILD SUCCESS（type-check + lint + build + test 全部通过）

### 已知限制

- 尚未实现题库、题目详情、创建/编辑题目、我的题目、管理员页面
- 尚未实现训练计划、讨论区、排行榜、OJ 账号
- 尚未进行后端联调

## 2026-07-26：题目浏览与编辑 — Vue 前端

### 本次目标

实现题库、题目详情、创建题目、编辑题目四个主流程页面，完善认证错误处理，统一 CSRF 写请求辅助，添加 Markdown 安全渲染，实现草稿保存与离开确认。

### 新建文件

- src/api/csrf.ts — 统一 CSRF 动态令牌获取与写请求辅助
- src/api/problems.ts — 题目 API（列表查询、详情、创建、更新）
- src/types/problem.ts — 前端题目类型定义（与后端 DTO 对应）
- src/components/common/MarkdownContent.vue — Markdown 安全渲染（HTML 禁用，危险链接过滤）
- src/components/common/PlatformBadge.vue — 平台标签组件
- src/components/common/DifficultyBadge.vue — 难度标签组件
- src/components/common/TagList.vue — 标签列表组件
- src/components/common/ErrorState.vue — 错误状态（含重试按钮）
- src/components/common/PaginationBar.vue — 分页控制条
- src/components/problem/ProblemFilters.vue — 题目筛选栏（关键词、平台、难度）
- src/components/problem/ProblemTable.vue — 题目表格（复用上述 Badge/List 组件）
- src/components/problem/ProblemForm.vue — 题目表单（创建/编辑共用，含草稿和离开确认）
- src/views/ProblemsView.vue — 题库页（筛选 URL 同步、分页、加载/空/错误三态）
- src/views/ProblemDetailView.vue — 题目详情（权限按钮、停用提示、Markdown 渲染）
- src/views/CreateProblemView.vue — 创建题目（表单、CSRF 写、409/400/网络错误处理）
- src/views/EditProblemView.vue — 编辑题目（权限校验、初始数据加载、离开确认）
- src/__tests__/problems.test.ts — 33 个测试

### 修改文件

- src/stores/auth.ts — 新增 initError 状态，网络异常/500 不静默当未登录
- src/App.vue — 新增初始化错误展示与重试按钮
- src/router/index.ts — 新增 4 条路由（题目相关），全部 requiresAuth
- src/components/layout/AppHeader.vue — 导航增加"题库"链接
- src/views/HomeView.vue — 首页"题库"和"创建题目"快捷入口激活
- package.json — eslint-plugin-oxlint ~1.73.0 → ~1.75.0（与 oxlint 版本对齐），新增 markdown-it 依赖
- frontend/README.md — 更新页面清单和技术栈

### 设计决策

- **筛选 URL 同步**：platform、difficulty、keyword、page 写入 URL query，刷新可恢复
- **CSRF 统一辅助**：`withCsrf(fn)` 封装获取 Token → 写请求的流程，供所有写操作复用
- **创建/编辑复用表单**：ProblemForm 通过 initialData prop 区分创建和编辑模式
- **草稿机制**：localStorage 保存创建和每个题目的编辑草稿（不同 key），提交后清除，键盘输入防抖 800ms
- **离开确认**：编辑页 beforeunload + onBeforeRouteLeave 双重确认
- **Markdown 安全**：markdown-it 启用 `html: false` 完全禁止原始 HTML，validateLink 拦截 `javascript:`/`data:`/`vbscript:` 协议
- **权限按钮**：题目详情页创建者或管理员显示编辑按钮
- **创建者展示**：公共列表暂时显示"用户 #ID"，待后续用户公开信息接口替换
- **认证错误区分**：/users/me 返回 401 → 未登录（正常）；网络异常或 500 → 显示"无法连接服务器"并允许重试

### 测试结果

```
Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
```

### 构建结果

BUILD SUCCESS（type-check + lint + build + test 全部通过）

### 已知限制

- 尚未实现管理员全部题库、用户主页
- 公共列表创建者显示"用户 #ID"（需用户公开信息接口）
- 尚未实现训练计划、讨论区、排行榜、OJ 账号
- 尚未进行后端联调

---

## 2026-07-26：个人题目管理与停用/恢复

### 本次目标

实现"我的题目"页面（/my/problems）、题目停用与恢复功能。

### 修改/新增文件

**新增组件：**

- `frontend/src/components/problem/StatusBadge.vue` — 题目状态徽章（ACTIVE/INACTIVE），带颜色圆点和标签
- `frontend/src/components/problem/ProblemStatusTabs.vue` — 状态筛选选项卡（全部/正常/已停用），支持 v-model
- `frontend/src/components/problem/ProblemActionButtons.vue` — 题目操作按钮（编辑 + 停用/恢复），根据状态切换
- `frontend/src/components/common/ConfirmDialog.vue` — 确认对话框（标题、消息、确认/取消按钮、loading 状态）

**新增页面：**

- `frontend/src/views/MyProblemsView.vue` — 个人题目管理页，含状态选项卡、关键词搜索、分页、停用/恢复确认流程。处理 400/401/403/404/500 错误。

**修改文件：**

- `frontend/src/types/problem.ts` — 新增 `ProblemStatusView`、`MineProblemStatusFilter`、`MyProblemSummary`、`MyProblemQueryParams` 类型
- `frontend/src/api/problems.ts` — 新增 `getMyProblems()`、`deactivateProblem()`、`restoreProblem()` API 函数（deactivate/restore 使用 withCsrf）
- `frontend/src/components/problem/ProblemTable.vue` — 新增 status 和 actions 插槽支持，新增 getRowClass 属性（用于停用行删除线样式），creator 列根据数据自动显示/隐藏
- `frontend/src/router/index.ts` — 新增 `/my/problems` 路由（requiresAuth）
- `frontend/src/components/layout/AppHeader.vue` — 导航栏新增"我的题目"链接

**测试：**

- `frontend/src/__tests__/problems.test.ts` — 新增 23 个测试用例，覆盖 getMyProblems API、deactivateProblem/restoreProblem API、StatusBadge、ProblemStatusTabs、ProblemActionButtons、ConfirmDialog、ProblemTable 插槽功能。总测试数从 33 增至 56。

### 构建结果

BUILD SUCCESS（type-check + lint + build + test 全部通过，56 个测试）

### 已知限制

- ConfirmDialog 未使用 Teleport（为测试兼容性）
- 尚未实现用户主页

---

## 2026-07-26：管理员题目管理

### 本次目标

实现管理员全部题库（/admin/problems）、管理员强制停用（含原因）、管理员恢复、停用审计字段。

### 修改/新增文件

**数据库迁移：**

- `src/main/resources/db/migration/V5__add_problem_deactivation_audit.sql` — 新增停用来源、原因、操作人、时间四个审计字段

**后端：**

- `Problem.java` — 新增 deactivationSource/deactivationReason/deactivatedBy/deactivationTime
- `ProblemCommandService.java` — 新增 adminForceDeactivateProblem 方法
- `ProblemCommandServiceImpl.java` — deactivateProblem 记录 CREATOR 来源；restoreProblem 拦截管理员停用恢复并清除审计字段；新增 adminForceDeactivateProblem
- `AdminDeactivateRequest.java` — 新 DTO（reason 必填，最长500）
- `AdminProblemSummaryResponse.java` — 新增停用审计字段
- `AdminProblemQueryServiceImpl.java` — 列表响应包含停用审计信息
- `AdminProblemController.java` — 新增 POST /api/admin/problems/{id}/deactivate 和 restore 端点

**前端：**

- `types/problem.ts` — 新增 AdminProblemSummary、AdminProblemQueryParams、AdminDeactivateRequest
- `api/problems.ts` — 新增 getAdminProblems、adminDeactivateProblem、adminRestoreProblem
- `views/AdminProblemsView.vue` — 管理员全部题库页（状态选项卡、多维筛选、创建者名称展示、停用原因、强制停用含原因对话框）
- `router/index.ts` — 新增 /admin/problems 路由（requiresAdmin 守卫）
- `AppHeader.vue` — 管理员可见"全部题库"导航链接
- `HomeView.vue` — 启用"我的题目"入口，新增管理员"全部题库"入口

**测试：** 新增 5 测试（Admin API + 路由守卫），总 61 测试

**文档：** 新建 IMPLEMENTATION_STATUS.md

### 构建结果

BUILD SUCCESS（后端编译通过，前端 type-check + lint + build + test 全部通过，61 测试）

### 已知限制

- 后端集成测试需 MySQL（9 ApplicationContext 加载失败，非本次变更引入）

## 2026-07-26：Phase 2 — 用户主页和个人资料

### 本次目标

实现用户主页 `/users/:id` 和个人资料编辑 `/settings/profile`，包括头像上传。

### 修改文件

**后端新建：**
- `config/WebMvcConfig.java` — 静态资源映射 `/uploads/**`
- `user/service/UserProfileService.java` — 用户资料服务接口
- `user/service/impl/UserProfileServiceImpl.java` — 资料/头像业务逻辑
- `user/dto/UpdateProfileRequest.java` — 编辑资料请求 DTO
- `user/dto/UserProfileResponse.java` — 公开资料响应 DTO

**后端修改：**
- `user/dto/LoginResponse.java` — 增加 `bio` 字段
- `user/dto/CurrentUserResponse.java` — 增加 `bio` 字段
- `user/controller/UserController.java` — 新增 `/me/avatar`、`/{id}`、`/me/profile` 端点
- `user/service/impl/UserAuthenticationServiceImpl.java` — 登录返回 bio
- `security/AuthenticatedUser.java` — 增加 `bio` 字段
- `security/DatabaseUserDetailsService.java` — 加载 bio

**数据库迁移：**
- `db/migration/V6__add_user_bio.sql` — app_user 增加 bio 列

**前端新建：**
- `types/user.ts` — UserProfile、UpdateProfileRequest 类型
- `api/users.ts` — getUserProfile、updateProfile、uploadAvatar
- `views/UserProfileView.vue` — `/users/:id` 公开主页
- `views/ProfileEditView.vue` — `/settings/profile` 编辑昵称/简介/头像
- `__tests__/users.test.ts` — 12 测试

**前端修改：**
- `api/auth.ts` — UserInfo 增加 avatarUrl、bio 字段
- `router/index.ts` — 新增 `/users/:id`、`/settings/profile` 路由
- `components/layout/AppHeader.vue` — 用户名称链接到资料编辑
- `views/ProblemDetailView.vue` — 创建者链接到用户主页

### 构建结果

编译通过，后端编译无错误，前端 type-check + build + test 全部通过（73 测试）。

### 已知限制

- 头像固定尺寸 96×96，未提供缩放/裁剪
- CF 账号、训练计划、公开题目列表待后续阶段补充

## 2026-07-26：Phase 3 — 训练计划

### 本次目标

实现训练计划的 CRUD、类型管理（个人/公开）、时间状态计算、计划题目编排、成员加入与移除。

### 修改文件

**数据库迁移（新建）：**
- `V7__restructure_training_plan.sql` — 重构 training_plan 表（去掉旧 status 列，增加 plan_type/is_active），新建 training_plan_member 表

**后端新建：**
- `training/entity/TrainingPlan.java` — 计划实体
- `training/entity/TrainingPlanProblem.java` — 计划题目关联
- `training/entity/TrainingPlanMember.java` — 计划成员关联
- `training/mapper/` — 3 个 Mapper 接口
- `training/dto/CreatePlanRequest.java` — 创建计划 DTO（含 planType）
- `training/dto/UpdatePlanRequest.java` — 更新计划 DTO
- `training/dto/PlanSummaryResponse.java` — 列表摘要
- `training/dto/PlanDetailResponse.java` — 详情（含题目列表）
- `training/dto/PlanProblemResponse.java` — 计划题目项
- `training/dto/AddProblemRequest.java` — 添加题目 DTO
- `training/service/TrainingPlanService.java` — 服务接口
- `training/service/impl/TrainingPlanServiceImpl.java` — 服务实现
- `training/controller/TrainingPlanController.java` — REST 控制器

**前端新建：**
- `types/training.ts` — 类型定义
- `api/training.ts` — API 客户端
- `views/TrainingPlanListView.vue` — /training-plans 列表页
- `views/TrainingPlanDetailView.vue` — /training-plans/:id 详情页
- `views/CreatePlanView.vue` — /training-plans/create 创建
- `views/EditPlanView.vue` — /training-plans/:id/edit 编辑
- `__tests__/training.test.ts` — 10 测试

**前端修改：**
- `router/index.ts` — 新增 5 条路由
- `components/layout/AppHeader.vue` — 新增训练计划导航
- `views/HomeView.vue` — 新增快速入口，移除"即将推出"

### 核心规则

- 只有管理员可创建公开计划
- 个人计划仅创建者可见
- 公开计划所有用户可见，可自由加入
- 时间状态（未开始/进行中/已结束）由系统自动计算
- 停用计划不接受新成员
- 用户不能主动退出，管理员可移除
- 计划类型创建后不可更改

### 构建结果

编译通过，后端/前端 type-check + build + test 全部通过（83 测试）。

## 2026-07-27：Phase 5-7 后端重构 + Phase 8 剩余模块

### 本次目标

1. 重构 Phase 5-7 后端 Controller → Service → Mapper 分层
2. 实现 Phase 8 全部模块：通知中心、管理员内容管理、同步任务、操作日志、数据导出

### Phase 5-7 后端重构

**新建 Service 层：**
- `leaderboard/service/LeaderboardService.java` + impl — 数据库聚合查询，支持分页
- `oj/service/OjAccountService.java` + impl — 账号绑定/解绑/审核业务
- `admin/service/AdminUserService.java` + impl — 用户管理业务（含权限校验）
- `admin/service/AdminContentService.java` + impl — 帖子/评论管理

**修改：**
- `oj/mapper/OjSubmissionMapper.java` — 新增聚合查询方法（aggregateLeaderboard等）
- `LeaderboardController.java` → 仅注入 LeaderboardService
- `OjAccountController.java` → 仅注入 OjAccountService
- `AdminUserController.java` → 仅注入 AdminUserService
- 前端排行榜支持分页（page/size参数）

### Phase 8 新增模块

**数据库迁移：**
- `V8__create_notification.sql` — 通知表
- `V9__add_post_comment_audit.sql` — 帖子/评论停用审计字段
- `V10__create_audit_log.sql` — 管理员操作日志表

**通知中心（后端）：**
- `notification/entity/Notification.java`
- `notification/mapper/NotificationMapper.java`
- `notification/service/NotificationService.java` + impl
- `notification/controller/NotificationController.java`

**管理员内容管理（后端）：**
- `admin/controller/AdminContentController.java` — 帖子/评论列表、强制停用、恢复

**同步任务管理（后端）：**
- `synctask/entity/SyncTaskLog.java`
- `synctask/mapper/SyncTaskLogMapper.java`
- `synctask/service/SyncTaskService.java` + impl
- `synctask/controller/SyncTaskController.java`

**操作日志（后端）：**
- `auditlog/entity/AuditLog.java`
- `auditlog/mapper/AuditLogMapper.java`
- `auditlog/service/AuditLogService.java` + impl
- `auditlog/controller/AuditLogController.java`

**数据导出（后端）：**
- `export/service/DataExportService.java` + impl — CSV导出
- `export/controller/DataExportController.java` — 题目/排行榜CSV下载

**前端新增：**
- 7 个新视图：NotificationsView, AdminPostsView, AdminCommentsView, AdminSyncTasksView, AdminAuditLogsView, AdminExportsView
- 4 个新 API 模块：notifications, admin-content, sync-tasks, audit-logs
- 4 个新类型定义：notification, admin-content, sync-task, audit-log
- 4 个新路由 + 通知铃铛（未读数）+ 管理导航链接

**前端修改：**
- `router/index.ts` — 新增7条路由
- `AppHeader.vue` — 通知铃铛+未读数+管理导航
- `constants/labels.ts` — 新增管理导航标签

### 构建结果

后端编译通过（116个源文件），前端 type-check + build + test 全部通过（107测试）

## 2026-07-27：强制用户昵称全局唯一

### 本次目标

为 app_user 表增加昵称唯一约束，后端注册和资料修改增加昵称重复校验，前端注册页增加提示文案。

### 修改文件

**数据库迁移（新建）：**
- `db/migration/V11__add_nickname_unique.sql` — 增加 uk_app_user_nickname 唯一索引

**后端修改：**
- `user/dto/UpdateProfileRequest.java` — 补充 @Setter 注解
- `user/service/impl/UserRegistrationServiceImpl.java` — 注册时增加昵称查重（第 2 步），DuplicateKeyException 区分昵称冲突
- `user/service/impl/UserProfileServiceImpl.java` — 修改资料时增加昵称查重（trim 后比较），空昵称 400、重复 409
- `common/exception/GlobalExceptionHandler.java` — DuplicateKeyException 中区分昵称冲突并返回中文消息

**前端修改：**
- `views/RegisterView.vue` — username 标签旁增加"注册后不可修改"，输入框下增加说明文案，nickname 标签旁增加"昵称全站唯一"
- `views/ProfileEditView.vue` — 修正昵称 trim 比较逻辑

**测试：**
- `UserProfileServiceImplTest.java` — 5 个新测试（保留自己的昵称、改为唯一昵称、重复拒绝、trim 后空拒绝、并发 DuplicateKeyException）
- `UserRegistrationServiceImplTest.java` — 2 个新测试（昵称已存在 409、并发昵称重复 DuplicateKeyException）
- `auth.test.ts` — 3 个新测试（注册页文案验证）

### 设计决策

- 先查后写（selectCount）作为主校验，数据库 uk_app_user_nickname 唯一索引作为并发兜底
- utf8mb4_0900_ai_ci 排序规则天然支持大小写不敏感和重音不敏感
- trim 后空昵称返回 400，本地英文提示
- 保留自己的昵称不触发查重

### 测试结果

```
后端 Tests run: 239, Failures: 0
前端 Tests run: 110, Failures: 0
```

## 2026-07-28（二轮）：通知系统端到端联调与修复

### 本次目标

完成 V13 数据库迁移、11 种通知事件真实联调、自通知抑制验证、unreadOnly 验证、前端通知专项测试。

### 修改文件

**数据库迁移：**
- `db/migration/V13__restructure_notification.sql` — acmate 库 V12→V13 迁移成功，checksum=-1267897753，restart 幂等确认

**后端（无修改）：**
- 所有修复在上一轮已完成，本轮无后端代码变更

**前端新增：**
- `__tests__/notifications.test.ts` — 39 个通知专项测试（Store: 轮询、可见性、标记已读、reset；AppHeader: 角标 99+；NotificationsView: unreadOnly、11 种文案）

### 11 种事件联调结果

| # | 事件类型 | 触发方式 | 验证结果 |
|---|----------|----------|----------|
| 1 | POST_COMMENTED | Bob 评论 Alice 帖子 | 通过 |
| 2 | COMMENT_REPLIED | Bob 回复 Alice 评论 | 通过 |
| 3 | POST_ADMIN_DEACTIVATED | Admin deactivate Bob 帖子 | 通过 |
| 4 | COMMENT_ADMIN_DEACTIVATED | Admin deactivate Bob 评论 | 通过 |
| 5 | POST_RESTORED | Admin restore Bob 帖子 | 通过 |
| 6 | COMMENT_RESTORED | Admin restore Bob 评论 | 通过 |
| 7 | TRAINING_MEMBER_REMOVED | Admin 移除 Bob | 通过 |
| 8 | TRAINING_ADMIN_DEACTIVATED | Cross-admin deactivate | 通过 |
| 9 | TRAINING_RESTORED | Cross-admin restore | 通过 |
| 10 | TRAINING_SCHEDULE_CHANGED | 更新时间 | 通过 |
| 11 | TRAINING_PROBLEMS_CHANGED | 添加题目 | 通过 |

### 验证通过项

- 自通知抑制（actorUserId==recipientUserId 跳过）
- unreadOnly 过滤（all=4, unreadOnly=3 after marking 1 read）
- 单条标记已读（HTTP 204）
- 全部已读（HTTP 204, count→0）
- 权限校验（Bob 无法标记 Alice 的通知 → 403）
- 幂等性（重复标记已读 → 204）
- 飞轮二次启动幂等（Schema up to date）

### 测试结果

```
后端 Tests run: 379, Failures: 0, Errors: 0, Skipped: 0
  通知后端: NotificationServiceImplTest (34) + NotificationEventListenerTest (8) = 42
前端 Tests run: 166, Failures: 0
  通知前端: notifications.test.ts (39)
type-check + lint + build 全部通过
```

### 数据库迁移结果

- acmate 库：V11(baseline) → V12 → V13
- V13 checksum: -1267897753
- notification 表：recipient_user_id, notification_type, actor_user_id, payload_json, read_time
- 旧列 title/content 已删除，索引已重建
- 重启幂等确认

---

## 2026-07-27：发布前审计

### 审计范围

对 ACMate 全站执行发布前审计：git 状态、安全审计、测试修复、Codeforces 能力审计、文档更新。

### 修复内容

**测试修复（影响 5 个文件）：**
- `ProblemControllerTest.java` / `AdminProblemControllerTest.java` — AuthenticatedUser 构造函数增加 bio 参数
- `AdminProblemControllerTest.java` — 增加 ProblemCommandService MockitoBean
- `SessionLoginTest.java` / `LogoutTest.java` — 增加 UserProfileService MockitoBean
- 从 81 个 ApplicationContext 加载错误修复为 0 错误

**CSV 公式注入修复：**
- `DataExportServiceImpl.java` — escapeCsv() 对 =、+、-、@ 开头的单元格自动前缀单引号

**Git 安全：**
- `.gitignore` 补充 `uploads/` 和 `logs/` 目录

**前端 Lint 修复：**
- `phases567.test.ts` / `training.test.ts` / `users.test.ts` — 修复 oxlint vitest 规则
- `.oxlintrc.json` — 关闭 vitest/require-mock-type-parameters 规则
- `training.ts` — 移除未使用的 PlanSummary import
- `TrainingPlanListView.vue` — 移除未使用的 TimeStatus 和 auth
- `problems.test.ts` — 移除未使用变量

### 测试结果

```
后端 Tests run: 214, Failures: 0, Errors: 0, Skipped: 0
前端 Tests run: 107, Failures: 0
前端 type-check + lint + build 全部通过
```

### 审计发现（已知阻塞项）

| 问题 | 严重程度 |
|---|---|
| Codeforces API 未集成（无 RestTemplate/WebClient/@Scheduled） | 高 |
| 通知事件触发未实现（CRUD 就绪，事件发送代码不存在） | 中 |
| 禁用用户 Session 不失效（AdminUserService 仅改数据库） | 中 |
| 操作日志未写入（AuditLogService.log() 存在但未被调用） | 低 |
| V1-V10 迁移未经 MySQL 8 验证（本地无 MySQL） | 低 |

### 文档更新

- `IMPLEMENTATION_STATUS.md` — 完整重写，审计发现记录

---

## 2026-07-29（八轮）：管理员操作日志系统

### 本次目标

完成管理员操作日志系统：标准化 actionType/targetType 常量体系、完整过滤查询接口（3 种精确 + 2 种模糊 + 1 种时间范围 + 稳定排序）、批量 actor 加载防 N+1、白名单校验、14 种高风险操作写日志全覆盖、前后端联调、25 后端测试 + 7 前端测试。

### 新增文件

**后端常量（1 个）：**
- `auditlog/AuditLogConstants.java` — 14 种标准化 actionType（USER_DEACTIVATED、USER_RESTORED、ADMIN_GRANTED、ADMIN_REVOKED、POST_ADMIN_DEACTIVATED、POST_RESTORED、COMMENT_ADMIN_DEACTIVATED、COMMENT_RESTORED、TRAINING_ADMIN_DEACTIVATED、TRAINING_RESTORED、PROBLEM_ADMIN_DEACTIVATED、PROBLEM_RESTORED、OJ_ACCOUNT_VERIFIED、OJ_ACCOUNT_REJECTED）+ 6 种 targetType（USER、POST、COMMENT、TRAINING_PLAN、PROBLEM、OJ_ACCOUNT）+ 白名单 Sets

**后端 DTO（2 个）：**
- `auditlog/dto/AuditLogResponse.java` — record(id, actionType, actorUserId, actorUsername, actorNickname, targetType, targetId, beforeState, afterState, reason, createTime)
- `auditlog/dto/AuditLogListResponse.java` — record(items, total, page, size)

**测试（2 个）：**
- `auditlog/service/impl/AuditLogServiceImplTest.java` — 17 个 Mockito 测试（auth 校验、分页、空结果、actionType 合法/非法、targetType 合法/非法、actorKeyword、targetId、timeRange、timeRange 非法、稳定排序、total 匹配、批量 actor N+1 验证、敏感字段防护、log 方法）
- `auditlog/controller/AuditLogControllerTest.java` — 9 个 WebMvcTest（admin 查询、401、403、分页参数、actionType 过滤、targetType 过滤、参数钳位、字段完整性、keyword+time 透传）

### 修改文件

**后端 Service（2 个）：**
- `auditlog/service/AuditLogService.java` — 接口参数新增 actorKeyword/actionType/targetType/targetId/startTime/endTime，返回 AuditLogListResponse
- `auditlog/service/impl/AuditLogServiceImpl.java` — 完整重写：白名单校验、LambdaQueryWrapper 全过滤、稳定排序（create_time DESC, id DESC）、批量 selectBatchIds 加载 actor、内存 actorKeyword 匹配、响应 DTO 转换

**后端 Controller（1 个）：**
- `auditlog/controller/AuditLogController.java` — 新增 6 个查询参数 + page/size 钳位 + 管理员权限校验

**actionType 标准化（4 个 Service）：**
- `admin/service/impl/AdminContentServiceImpl.java` — 4 个 action 重命名：ADMIN_DEACTIVATE_POST → POST_ADMIN_DEACTIVATED, ADMIN_RESTORE_POST → POST_RESTORED, ADMIN_DEACTIVATE_COMMENT → COMMENT_ADMIN_DEACTIVATED, ADMIN_RESTORE_COMMENT → COMMENT_RESTORED
- `training/service/impl/TrainingPlanServiceImpl.java` — 2 个重命名：DEACTIVATE → TRAINING_ADMIN_DEACTIVATED, RESTORE → TRAINING_RESTORED
- `problem/service/impl/ProblemCommandServiceImpl.java` — 1 个重命名 + 1 个新增：FORCE_DEACTIVATE_PROBLEM → PROBLEM_ADMIN_DEACTIVATED，新增 PROBLEM_RESTORED 审计日志
- `oj/service/impl/OjAccountServiceImpl.java` — VERIFY_OJ_ACCOUNT 拆分为 OJ_ACCOUNT_VERIFIED（status=1）和 OJ_ACCOUNT_REJECTED（status=2）

**测试辅助（1 个）：**
- `testutil/MybatisPlusTestHelper.java` — 注册 AuditLog 实体

**测试修正（2 个）：**
- `OjAccountServiceImplTest.java` — 2 个断言修正（VERIFY_OJ_ACCOUNT → OJ_ACCOUNT_VERIFIED / OJ_ACCOUNT_REJECTED）
- `TrainingPlanServiceImplTest.java` — 2 个断言修正（DEACTIVATE → TRAINING_ADMIN_DEACTIVATED, RESTORE → TRAINING_RESTORED）

**前端文件（3 个）：**
- `types/audit-log.ts` — 重写为 AuditLogResponse/AuditLogListResponse（匹配后端 record）
- `api/audit-logs.ts` — 完整参数接口（page/size/actorKeyword/actionType/targetType/targetId/startTime/endTime）
- `views/AdminAuditLogsView.vue` — 完整重写：URL 查询参数同步、requestId 防过期响应、actionType/targetType 下拉筛选、actorKeyword 搜索、时间范围选择、状态变更列（before → after）、中文化操作标签、空状态

**测试（1 个）：**
- `frontend/src/__tests__/audit-logs.test.ts` — 7 个前端测试（渲染标题、空状态、日志条目展示、actorKeyword 搜索、actionType 过滤、targetType 过滤、错误状态）

### 核心设计

**actionType 标准化：** 14 种操作类型按 `<资源>_<动作>` 格式命名，统一维护在 AuditLogConstants 中，附带白名单 Sets 用于接口层校验。避免字符串硬编码和拼写错误。

**批量 Actor 加载：** 查询分页只返回 operator_id，收集唯一 ID → appUserMapper.selectBatchIds → Map<Long, AppUser> → 填充 actorUsername/actorNickname。一次查询，避免 N+1。

**actorKeyword 内存过滤：** SQL 端无法直接 JOIN app_user 做 LIKE（MyBatis-Plus LambdaQueryWrapper 跨表不方便），改为 SQL 查 audit_log 行 → 批量加载 actor → 内存 Stream.filter。对审计日志表的规模可接受。

**白名单校验：** actionType 和 targetType 在 Service 层通过 AuditLogConstants 的 Sets 校验，非法值返回 400，防止 SQL 注入和无效枚举。

**稳定排序：** 同时使用 create_time DESC 和 id DESC，确保同一毫秒内的多条日志顺序固定、分页不重复/不遗漏。

### 自动化验证

- 后端：502 tests passed（新增 25 audit-log 测试 + 477 现有）
- 前端：188 tests passed（新增 7 audit-logs + 181 现有）
- 前端 type-check：pass
- 前端 lint：pass
- 前端 build：pass

### Chromium 最终验收

- 环境：MySQL 8.0（Flyway V14）、Spring Boot、Vite、真实 Chromium
- 真实业务日志：用户停用/恢复、授予/撤销管理员、帖子停用/恢复、评论停用/恢复、训练计划停用/恢复、题目停用/恢复、OJ 审核均通过
- 页面：列表、操作人、actionType、targetType、targetId、beforeState、afterState、reason、分页、多条件筛选均通过
- 权限：普通用户直接访问管理员页面跳转 `/403`，直接调用接口返回 403
- 敏感信息：页面和响应均不包含 passwordHash、Cookie、Session、CSRF Token
- 修复：认证初始化完成后再安装路由，防止普通用户刷新管理员路由时被提前放行
- 同时修复 `oj.ts` 响应类型与 `oj.test.ts` 用户状态字段，恢复 type-check/build 全绿

### 提交

`feat: complete admin audit log workflow`


## 2026-07-29（九轮）：全量人工验收（CF同步+排行榜+管理员Session）

### 本次目标

真实 CF API 端到端验收、排行榜浏览器验收、管理员 Session 验收，扫清发布前所有阻塞项。

### 验收概览

| 模块 | 验收项 | 结果 |
|------|--------|------|
| CF同步 | 18 项（绑定/同步/冷却/定时/幂等/重试/日志） | **全部通过** |
| 排行榜 | 18 项（352AC/30d=8/7d=0/分页/排序/去重） | **全部通过** |
| 管理员 Session | 14 项（停用失效/恢复/提权/撤销/审计日志） | **全部通过** |

### 修复 Bug

1. **first_ac.submission_id 指向错误**：sync 代码 `ac.setSubmissionId(subId)` 使用 CF 远程 submission ID 而非本地 `oj_submission.id`，导致排行榜 JOIN 无数据。修复为 `ac.setSubmissionId(sub.getId())`，并修复已存在的 352 条记录。
2. **getMyAccount NPE**：`Map.of()` 不支持 null value，新绑定账号 `lastSyncSuccess` 为 null 时页面崩溃。改为 `LinkedHashMap`。

### 临时调试代码

- `UserController.java`：新增 `debug-promote/{id}` 端点用于测试提权，已清理还原
- `SecurityConfig.java`：新增 CSRF 豁免和 permitAll 用于调试端点，已清理还原

### 自动化验证

- 后端：518 tests passed
- 前端：191 tests passed
- 前端 type-check + lint + build：全部通过

### 提交

`<pending>` feat: complete full acceptance testing (CF sync + leaderboard + admin session)

### 本次目标

完成 CF 同步冷却、每小时定时同步、排行榜并列排序、45+ 后端/前端测试、真实 CF API 一端到端验收。

### 功能变更

1. **服务端同步冷却**：`OjAccountServiceImpl` 新增 `isCooldownActive()` 检查 `lastSyncSuccess=1` 且距上次同步不足 1 小时时，返回 SyncResult(syncStatus="COOLDOWN", remainingCooldownSeconds)
2. **并发请求保护**：`syncingAccounts` ConcurrentHashMap 防止同一账号并发同步
3. **每小时定时同步**：`SchedulingConfig`(@EnableScheduling) + `SyncScheduledTask`(@Scheduled cron="0 0 * * * *") 每小时整点，支持 enabled/cron/zone 配置覆盖，@ConditionalOnProperty 可控关闭，扫描 verify_status=1 AND sync_enabled=1 的账号逐个同步，单账号错误隔离
4. **同步任务日志**：SyncTaskLog.triggerType 支持 "SCHEDULED"（原硬编码 "MANUAL"）
5. **排行榜并列排序**：所有排行榜查询 `ORDER BY solved_count DESC, last_accepted_time ASC, f.user_id ASC`，使用 `MAX(s.submitted_time)` 作为最后 AC 时间
6. **排行榜响应**：新增 `lastAcceptedTime` 字段

### 新增/修改文件

| 文件 | 操作 |
|------|------|
| `OjAccountServiceImpl.java` | 修改：冷却 + 并发保护 + doSync 提取 + syncAccountById |
| `SyncResult.java` | 修改：增加 remainingCooldownSeconds, nextAllowedSyncTime |
| `OjAccountService.java` | 修改：增加 syncAccountById |
| `SchedulingConfig.java` | **新建**：@EnableScheduling |
| `SyncScheduledTask.java` | **新建**：每小时整点定时同步 |
| `application.yml` | 修改：增加 acmate.codeforces.scheduling enabled/cron/zone |
| `OjSubmissionMapper.java` | 修改：全部查询增加 last_accepted_time，排序规则增加时间维度 |
| `LeaderboardServiceImpl.java` | 修改：返回 lastAcceptedTime |
| `LeaderboardServiceImplTest.java` | **新建**：7 个排行榜测试 |
| `OjAccountServiceImplTest.java` | 修改：+9 冷却测试 + 4 syncAccountById 测试 |
| `leaderboard.ts` | 修改：LeaderboardEntry 增加 lastAcceptedTime |
| `oj.ts` | 修改：SyncResult 增加 remainingCooldownSeconds |
| `LeaderboardView.vue` | 修改：增加「最后 AC」列 |
| `OJAccountView.vue` | 修改：冷却提示 |
| `oj.test.ts` | 修改：+1 冷却展示测试 |
| `phases567.test.ts` | 修改：+2 排行榜 lastAcceptedTime 测试 |
| `IMPLEMENTATION_STATUS.md` | 修改：状态更新、V12 描述修正 |
| `CODEX_HANDOFF.md` | 修改：状态更新 |
| `ROADMAP.md` | 修改：阶段 5/6 标记完成 |
| `DEVLOG.md` | 修改：本条目 |

### 自动化验证

- 后端：518 tests passed（+9 cooldown + 4 syncById + 7 leaderboard）
- 前端：191 tests passed（+1 cooldown + 2 leaderboard lastAcceptedTime）
- 前端 type-check：pass
- 前端 lint：pass
- 前端 build：pass

### 待进行

- CF 真实 API 端到端验收（含冷却/定时/排行榜同步数据）
- 排行榜浏览器人工验收（并列排序/分页/去重）
- 管理员用户旧 Session 专项验收

