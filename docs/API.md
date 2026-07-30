# API

## POST /api/auth/register

用户注册。

### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| username | string | 是 | 4-32 位，仅允许英文字母、数字、下划线，正则 `^[A-Za-z0-9_]{4,32}$` |
| password | string | 是 | 8-64 位 |
| nickname | string | 是 | 去除首尾空格后 2-32 位 |
| email | string | 否 | 非空时必须为合法邮箱格式，空字符串规范化为 null |

### 规范化规则

- username：去除首尾空格，转为小写
- nickname：去除首尾空格
- email：为空或纯空格时规范化为 null，否则去除首尾空格并转为小写
- password：不 trim，不记录明文

### 成功响应

**201 Created**

```json
{
  "id": 1,
  "username": "testuser1",
  "nickname": "Test User",
  "email": "test1@example.com"
}
```

### 错误响应

**400 Bad Request** — 字段校验失败

```json
{
  "code": 400,
  "message": "请求参数校验失败",
  "timestamp": "2026-07-24T19:28:58.4498477",
  "fieldErrors": [
    {"field": "username", "message": "需要匹配正则表达式\"^[A-Za-z0-9_]{4,32}$\""}
  ]
}
```

**409 Conflict** — 用户名、昵称或邮箱已存在

```json
{
  "code": 409,
  "message": "用户名已被使用",
  "timestamp": "2026-07-24T19:28:49.1197739"
}
```

昵称冲突：

```json
{
  "code": 409,
  "message": "该昵称已被使用，请更换其他昵称。",
  "timestamp": "2026-07-27T15:54:15.8456861"
}
```

并发重复注册时返回通用信息：

```json
{
  "code": 409,
  "message": "用户名或邮箱已被使用",
  "timestamp": "2026-07-24T19:29:00.0000000"
}
```

## POST /api/auth/login

Session 登录。

### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| username | string | 是 | 4-32 位，仅允许英文字母、数字、下划线 |
| password | string | 是 | 8-64 位 |

### 规范化规则

- username：去除首尾空格，转为小写
- password：不 trim

### 成功响应

**200 OK**，同时设置 JSESSIONID Cookie，后续请求需携带此 Cookie。

```json
{
  "id": 1,
  "username": "testuser",
  "nickname": "Test User",
  "email": "test@example.com",
  "avatarUrl": null,
  "admin": false
}
```

### 错误响应

**400 Bad Request** — 字段校验失败

**401 Unauthorized** — 用户名或密码错误

```json
{"code": 401, "message": "用户名或密码错误", "timestamp": "..."}
```

**403 Forbidden** — 账号已被禁用

```json
{"code": 403, "message": "账号已被禁用", "timestamp": "..."}
```

### CSRF

**500 Internal Server Error** — 认证基础设施异常

```json
{"code": 500, "message": "服务器内部错误，请稍后再试", "timestamp": "..."}
```

### CSRF

登录 POST 不需要 CSRF Token。

### 认证流程

1. UsernamePasswordAuthenticationToken 构建
2. DaoAuthenticationProvider 通过 DatabaseUserDetailsService 加载用户
3. BCryptPasswordEncoder 验证密码
4. 认证成功后 SecurityContext 写入 HttpSession
5. ChangeSessionIdAuthenticationStrategy 更换 Session ID（防 Session Fixation）
6. 返回用户信息

### 认证错误状态码总结

| 场景 | HTTP | 响应 |
|---|---|---|
| 用户名或密码错误 | 401 | `用户名或密码错误` |
| 用户不存在 | 401 | `用户名或密码错误` |
| 账号已禁用 | 403 | `账号已被禁用` |
| DB/基础设施异常 | 500 | `服务器内部错误，请稍后再试` |

## GET /api/users/me

获取当前登录用户信息。

### 认证

需要携带登录时获得的 JSESSIONID Cookie。

### 成功响应

**200 OK**

```json
{
  "id": 1,
  "username": "testuser",
  "nickname": "Test User",
  "email": "test@example.com",
  "avatarUrl": null,
  "admin": false
}
```

### 错误响应

**401 Unauthorized** — 未登录或 Session 已失效

```json
{"code": 401, "message": "未登录或登录已失效", "timestamp": "..."}
```

### 实现说明

- 使用 @AuthenticationPrincipal 直接获取 AuthenticatedUser
- 不为此接口重新查询数据库

## GET /api/auth/csrf

获取当前 Session 的 CSRF Token。

### 认证

不需要登录，但请求必须携带 JSESSIONID Cookie 才能与后续写操作共享同一个 CSRF Token。

### CSRF

GET 请求不受 CSRF 保护。不要求携带 CSRF Token。

### 成功响应

**200 OK**

```json
{
  "token": "<csrf-token>",
  "headerName": "<header-name>",
  "parameterName": "<parameter-name>"
}
```

| 字段 | 说明 |
|------|------|
| token | CSRF Token 值，用于后续写操作的请求头 |
| headerName | 请求头名称，用于提交 CSRF Token |
| parameterName | 表单参数名称（使用请求头即可以忽略） |

### 客户端流程

1. 登录 POST /api/auth/login 并保留 JSESSIONID Cookie
2. 使用同一客户端 GET /api/auth/csrf，读取 token 和 headerName
3. 调用 POST /api/auth/logout 时，以 headerName 为请求头键、token 为请求头值
4. logout 成功返回 204

## POST /api/auth/logout

退出登录，使当前 Session 失效。

### 认证

需要携带登录时获得的 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。通过 GET /api/auth/csrf 获取。不在 CSRF 忽略列表中。

### 成功响应

**204 No Content** — 响应体为空。SecurityContext 已清除，Session 已失效。

### 错误响应

**401 Unauthorized** — 未登录（当前未认证，即使用了有效 CSRF Token）

```json
{"code": 401, "message": "未登录或登录已失效", "timestamp": "..."}
```

**403 Forbidden** — 缺少或无效 CSRF Token（已认证但无 CSRF）

```json
{"code": 403, "message": "无权执行该操作", "timestamp": "..."}
```

### 401 vs 403 区别

| 状态 | 场景 |
|------|------|
| 401 | 未认证用户调用 logout（/me 也是 401） |
| 403 | 已认证但缺少/无效 CSRF Token |

### 实现说明

- 使用 SecurityContextLogoutHandler 清除认证
- 同时清除 SecurityContext 和 HttpSession
- 不接受任何请求体或参数
- 退出后旧 Session 无法访问任何受保护接口

## PUT /api/users/me/profile

修改当前用户的昵称和个人简介。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| nickname | string | 否 | 2-32 字符，去除首尾空格后不能为空 |
| bio | string | 否 | 最多 500 字符 |

### 规范化规则

- nickname：去除首尾空格
- bio：去除首尾空格

### 成功响应

**200 OK** — 响应体为空。

### 错误响应

**400 Bad Request** — 昵称格式无效或去除空格后为空

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token

**409 Conflict** — 昵称已被其他用户使用

```json
{"code": 409, "message": "该昵称已被使用，请更换其他昵称。", "timestamp": "..."}
```

### 规则

- 昵称与当前昵称相同时不检查唯一性
- 昵称去除首尾空格后与当前昵称相同时视为未修改
- 数据库唯一索引 `uk_app_user_nickname` 作为并发安全兜底

## GET /api/problems/{id}

获取题目详情。

### 认证

需要登录后携带 JSESSIONID Cookie。

### 可见性规则

- status=1（正常）：所有已登录用户可见
- status=0（停用）：仅题目创建者和管理员可见；其他用户返回 404，不暴露停用题目存在性

### 成功响应

**200 OK**

```json
{
  "id": 1,
  "platform": "CUSTOM",
  "externalProblemKey": "EXT-1",
  "title": "Two Sum",
  "sourceUrl": "https://example.com/1",
  "difficulty": "800",
  "tags": "dp,greedy",
  "contentMd": "## 题目描述\n...",
  "creatorUserId": 1,
  "createTime": "2026-07-20T12:00:00",
  "updateTime": "2026-07-21T12:00:00"
}
```

### 错误响应

**401 Unauthorized** — 未登录

**404 Not Found** — 题目不存在或停用题目无查看权限（统一返回"题目不存在"）

```json
{"code": 404, "message": "题目不存在", "timestamp": "..."}
```

### 实现说明

- 先根据 id 查询题目，再根据 status 和查看者身份判断可见性
- 不暴露 status 字段

## GET /api/problems/mine

查询当前登录用户创建的题目，支持按状态筛选。只能查看自己的题目，不能指定其他用户 ID。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

GET 请求不需要 CSRF Token。

### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | long | 1 | 页码，最小 1 |
| size | long | 20 | 每页数量，1-100 |
| status | string | ALL | 状态筛选：ALL / ACTIVE / INACTIVE |
| platform | string | — | 平台筛选：CUSTOM / CODEFORCES / NOWCODER / OTHER |
| difficulty | string | — | 难度精确匹配 |
| keyword | string | — | 关键词，匹配 title 或 externalProblemKey，最大 100 字符 |

示例：`?status=ACTIVE&page=1&size=20`

### 成功响应

**200 OK**

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "pages": 1,
  "records": [
    {
      "id": 1,
      "platform": "CUSTOM",
      "externalProblemKey": "EXT-1",
      "title": "Two Sum",
      "sourceUrl": "https://example.com/1",
      "difficulty": "800",
      "tags": "dp,greedy",
      "status": "ACTIVE",
      "createTime": "2026-07-25T12:00:00",
      "updateTime": "2026-07-26T12:00:00"
    },
    {
      "id": 2,
      "platform": "CODEFORCES",
      "externalProblemKey": "123A",
      "title": "Watermelon",
      "sourceUrl": null,
      "difficulty": "800",
      "tags": null,
      "status": "INACTIVE",
      "createTime": "2026-07-24T10:00:00",
      "updateTime": "2026-07-25T15:30:00"
    }
  ]
}
```

相比公共列表，多了 `status`（ACTIVE/INACTIVE 字符串）和 `updateTime`，但移除了 `creatorUserId`（全是当前用户）。

### 错误响应

**400 Bad Request** — 参数校验失败（page < 1、size > 100、platform 非法、status 非法）

**401 Unauthorized** — 未登录

### 过滤规则

- 固定 `creator_user_id = 当前认证用户 ID`，不接受客户端传入
- ALL：不附加 status 条件
- ACTIVE：附加 status=1
- INACTIVE：附加 status=0
- platform、difficulty、keyword 复用公共列表筛选语义
- 按 createTime DESC、id DESC 排序

### 实现说明

- creatorUserId 来自 @AuthenticationPrincipal，不能由请求参数指定（信任边界）
- 公共列表不返回 status，避免向其他用户暴露题目管理状态
- MineProblemStatusFilter 枚举提供语义化筛选，不暴露数据库数字

## GET /api/problems

分页查询题目列表。

### 认证

需要登录后携带 JSESSIONID Cookie。

### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | long | 1 | 页码，最小 1 |
| size | long | 20 | 每页数量，1-100 |
| platform | string | — | 平台筛选：CUSTOM / CODEFORCES / NOWCODER / OTHER |
| difficulty | string | — | 难度精确匹配 |
| keyword | string | — | 关键词，匹配 title 或 externalProblemKey，最大 100 字符 |
| creatorUserId | long | — | 创建者 ID，仅返回该创建者的公开题目 |

示例：`?page=1&size=20&platform=CODEFORCES&difficulty=1200&keyword=two&creatorUserId=5`

### 成功响应

**200 OK**

```json
{
  "page": 1,
  "size": 20,
  "total": 42,
  "pages": 3,
  "records": [
    {
      "id": 1,
      "platform": "CUSTOM",
      "externalProblemKey": "EXT-1",
      "title": "Two Sum",
      "sourceUrl": "https://example.com/1",
      "difficulty": "800",
      "tags": "dp,greedy",
      "creatorUserId": 1,
      "createTime": "2026-07-20T12:00:00"
    }
  ]
}
```

### 错误响应

**400 Bad Request** — 参数校验失败（page < 1、size > 100、platform 非法、keyword 超长）

**401 Unauthorized** — 未登录

### 过滤规则

- 固定过滤 status=1
- platform 非空时精确匹配
- difficulty 非空时精确匹配
- keyword 非空时匹配 title 或 externalProblemKey（括号组合，不会绕过 status 过滤）
- 按 createTime DESC、id DESC 排序

### 实现说明

- status=0 的题目在任何条件下都不会出现在列表中
- 使用 MyBatis-Plus 分页插件，数据库层兜底 maxLimit=100

## GET /api/admin/problems

管理员查询全站题目，支持按状态、创建者、平台、难度、关键词筛选。响应中包含创建者信息（username、nickname），由服务端批量加载。

### 认证

需要管理员（ROLE_ADMIN）角色。

### CSRF

GET 请求不需要 CSRF Token。

### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | long | 1 | 页码，最小 1 |
| size | long | 20 | 每页数量，1-100 |
| status | string | ALL | 状态筛选：ALL / ACTIVE / INACTIVE |
| creatorUserId | long | — | 创建者 ID 精确匹配 |
| platform | string | — | 平台筛选：CUSTOM / CODEFORCES / NOWCODER / OTHER |
| difficulty | string | — | 难度精确匹配 |
| keyword | string | — | 关键词，匹配 title 或 externalProblemKey，最大 100 字符 |

示例：`?status=ACTIVE&platform=CODEFORCES&keyword=two`

### 成功响应

**200 OK**

```json
{
  "page": 1,
  "size": 20,
  "total": 2,
  "pages": 1,
  "records": [
    {
      "id": 1,
      "platform": "CUSTOM",
      "externalProblemKey": "EXT-1",
      "title": "Two Sum",
      "sourceUrl": "https://example.com/1",
      "difficulty": "800",
      "tags": "dp,greedy",
      "status": "ACTIVE",
      "creatorUserId": 10,
      "creatorUsername": "testuser",
      "creatorNickname": "Test User",
      "createTime": "2026-07-25T12:00:00",
      "updateTime": "2026-07-26T12:00:00"
    }
  ]
}
```

### 错误响应

**400 Bad Request** — 参数校验失败（invalid status/creatorUserId ≤ 0）

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员

### 过滤规则

- ALL：不附加 status 条件，查看正常和停用全部题目
- ACTIVE：附加 status=1
- INACTIVE：附加 status=0
- creatorUserId、platform、difficulty 精确匹配
- keyword 括号组合匹配 title 或 externalProblemKey
- 按 createTime DESC、id DESC 排序

### 实现说明

- 创建者信息（username、nickname）通过 selectBatchIds 批量加载，避免 N+1
- 创建者数据缺失时 username 和 nickname 返回 null，不抛异常
- 与 /api/problems/mine 共享 MineProblemStatusFilter 枚举
- 权限在 SecurityFilterChain 中通过 hasRole("ADMIN") 控制

## POST /api/problems

由当前登录用户创建题目。普通用户和管理员都可以创建，创建后普通用户成为该题目的创建者。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| platform | string | 是 | CUSTOM / CODEFORCES / NOWCODER / OTHER |
| externalProblemKey | string | 否 | 最大 64 字符，非 CUSTOM 平台必填 |
| title | string | 是 | 非空，最大 255 字符 |
| sourceUrl | string | 否 | 最大 1024 字符，空字符串规范化为 null |
| difficulty | string | 否 | 最大 32 字符，空字符串规范化为 null |
| tags | string | 否 | 最大 255 字符，逗号分隔，自动去重去空 |
| contentMd | string | 否 | Markdown 内容，最大 2097152 字符（约 2MB） |

### 请求体限制

以下字段由服务端指定，请求体中即使携带也会被忽略：
- `creatorUserId` — 服务端从当前认证用户获取
- `status` — 服务端固定为 1（正常）
- `id` / `createTime` / `updateTime` — 由数据库自动生成

### 规范化规则

- platform：转为大写
- externalProblemKey：去除首尾空格，空字符串规范化为 null
- title：去除首尾空格
- sourceUrl / difficulty：去除首尾空格后空字符串规范化为 null
- tags：按逗号拆分 → 去空格 → 去空 → 去重（保留首次出现顺序）→ 逗号连接；空结果规范化为 null
- contentMd：不 trim（Markdown 首尾空白可能有意为之）

### 成功响应

**201 Created**

```json
{
  "id": 1,
  "platform": "CUSTOM",
  "externalProblemKey": null,
  "title": "Two Sum",
  "sourceUrl": "https://example.com",
  "difficulty": "800",
  "tags": "dp,array",
  "contentMd": "## 题目描述\n...",
  "creatorUserId": 1,
  "createTime": "2026-07-25T12:00:00",
  "updateTime": "2026-07-25T12:00:00"
}
```

响应中不包含 status 字段。

### 错误响应

**400 Bad Request** — 字段校验失败或平台+题目标识为空（非 CUSTOM 平台）

```json
{"code": 400, "message": "非自定义平台必须提供外部题目标识", "timestamp": "..."}
```

**401 Unauthorized** — 未登录

```json
{"code": 401, "message": "未登录或登录已失效", "timestamp": "..."}
```

**403 Forbidden** — 缺少/无效 CSRF Token

```json
{"code": 403, "message": "无权执行该操作", "timestamp": "..."}
```

**409 Conflict** — 相同平台+题目标识的题目已存在

```json
{"code": 409, "message": "该平台题目标识已存在", "timestamp": "..."}
```

### 实现说明

- creatorUserId 来自 @AuthenticationPrincipal，不由请求体指定（信任边界）
- status 由服务端固定为 1，不在请求中暴露
- 前置查重（selectCount by platform + externalProblemKey）提供友好错误信息
- 数据库唯一索引 uk_platform_problem 作为并发兜底，只捕获 DuplicateKeyException
- 权限控制在 SecurityFilterChain 中使用 authenticated()，所有登录用户均可创建
- 后续资源级管理操作（修改、停用等）将在获取资源后判断"创建者或管理员"

## PUT /api/problems/{id}

由题目创建者或管理员完整修改题目。创建者和管理员可以编辑正常或停用题目；编辑停用题目不会自动恢复 status。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| platform | string | 是 | CUSTOM / CODEFORCES / NOWCODER / OTHER |
| externalProblemKey | string | 否 | 最大 64 字符，非 CUSTOM 平台必填 |
| title | string | 是 | 非空，最大 255 字符 |
| sourceUrl | string | 否 | 最大 1024 字符，空字符串规范化为 null |
| difficulty | string | 否 | 最大 32 字符，空字符串规范化为 null |
| tags | string | 否 | 最大 255 字符，逗号分隔，自动去重去空 |
| contentMd | string | 否 | Markdown 内容，最大 2097152 字符（约 2MB） |

### 请求体限制

以下字段由服务端指定或不可修改，请求体中即使携带也会被忽略：
- `creatorUserId` — 不可修改，由创建时确定
- `status` — 不可通过此接口修改（编辑停用题目不会自动恢复）
- `id` / `createTime` / `updateTime` — 由数据库维护

### 规范化规则

与 POST /api/problems 完全一致。所有字符串字段经过 ProblemFieldNormalizer 统一处理。

### 所有权校验

- 普通用户只能修改 `creatorUserId == operatorUserId` 的题目
- 管理员可以修改任意正常或停用的题目
- 非创建者非管理员：对正常题目返回 403，对停用题目返回 404（不暴露存在性）

### 成功响应

**200 OK** — 返回更新后重新查询的题目详情（含最新 updateTime）

### 错误响应

**400 Bad Request** — 字段校验失败或非 CUSTOM 平台未提供题目标识

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或无资源管理权限（正常题目）

**404 Not Found** — 题目不存在或停用题目无管理权限（不区分）

**409 Conflict** — 相同平台+题目标识已被其他题目使用（排除自身）

### 实现说明

- 使用 LambdaUpdateWrapper.set() 显式设置字段，确保 null 值可写入
- UPDATE WHERE 不再固定 status=1，允许编辑停用题目
- 编辑停用题目仅修改内容字段，不会自动恢复 status
- 题目标识查重时排除自身（.ne(Problem::getId, problemId)）
- 更新后重新查询确保响应反映数据库实际状态（含 ON UPDATE CURRENT_TIMESTAMP）
- @Transactional 保证查询→校验→更新→回读的读写边界一致
- 权限判断在 Service 层完成，SecurityFilterChain 仅要求 authenticated()

## POST /api/problems/{id}/deactivate

停用题目（ACTIVE → INACTIVE）。创建者和管理员可以操作。停用是逻辑操作，不会物理删除记录。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 权限

- 题目创建者可以停用自己的题目
- 管理员可以停用任意题目
- 其他普通用户对正常题目返回 403，对停用题目返回 404（不暴露存在性）

### 幂等

- ACTIVE → INACTIVE，返回 204
- INACTIVE → 直接返回 204，不重复更新

### 成功响应

**204 No Content** — 响应体为空。

### 错误响应

**400 Bad Request** — 题目 ID 或操作者 ID 无效

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或无管理权限（正常题目）

**404 Not Found** — 题目不存在或停用题目无管理权限

### 实现说明

- 先查询资源再判断权限——URL 层无法确定创建者
- UPDATE WHERE 包含 status=1，防止并发重复停用
- 停用后题目仍占用 platform+externalProblemKey 唯一性
- 停用题目不删除，创建者可随时查看、编辑或恢复
- 停用题目不出现在公共题库中

## POST /api/problems/{id}/restore

恢复题目（INACTIVE → ACTIVE）。创建者和管理员可以操作。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 权限

- 题目创建者可以恢复自己的题目
- 管理员可以恢复任意题目
- 其他普通用户对正常题目返回 403，对停用题目返回 404

### 幂等

- INACTIVE → ACTIVE，返回 204
- ACTIVE → 直接返回 204，不重复更新

### 成功响应

**204 No Content** — 响应体为空。

### 错误响应

**400 Bad Request** — 题目 ID 或操作者 ID 无效

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或无管理权限（正常题目）

**404 Not Found** — 题目不存在或停用题目无管理权限

### 实现说明

- UPDATE WHERE 包含 status=0，防止并发重复恢复
- 恢复后的题目重新出现在公共题库中

---

## 管理员用户管理 API

### GET /api/admin/users

管理员分页查询用户列表，支持搜索和筛选。

#### 认证

需要管理员（ROLE_ADMIN）角色。

#### CSRF

GET 请求不需要 CSRF Token。

#### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，最小 1 |
| size | int | 20 | 每页数量，1-100 |
| keyword | string | — | 关键词，匹配 username 或 nickname（模糊搜索） |
| status | string | — | 状态筛选：ACTIVE（正常）/ INACTIVE（已禁用） |
| admin | string | — | 角色筛选：ADMIN（管理员）/ USER（普通用户） |

示例：`?page=1&size=20&keyword=alice&status=ACTIVE&admin=USER`

#### 成功响应

**200 OK**

```json
{
  "users": [
    {
      "id": 1,
      "username": "admin",
      "nickname": "Admin",
      "email": "admin@acmate.com",
      "avatarUrl": null,
      "bio": null,
      "admin": true,
      "status": 1,
      "createTime": "2026-01-01T00:00:00",
      "lastLoginTime": null
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

#### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员

#### 字段说明

- `admin`：boolean，表示是否为管理员
- `status`：1=正常，0=已禁用
- 不返回 `passwordHash` 字段

#### 排序规则

按 `createTime DESC, id DESC` 排序，确保分页稳定性。

---

### PUT /api/admin/users/{id}/deactivate

管理员停用用户。停用后目标用户全部现有 Session 立即失效，不能继续访问受保护接口。

#### 认证

需要管理员（ROLE_ADMIN）角色。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| reason | string | 是 | 去除首尾空格后不能为空，最长 500 字符 |

#### 成功响应

**204 No Content**

#### 幂等

- 已停用用户重复停用返回 204，不重复更新，不重复写 audit_log

#### 错误响应

**400 Bad Request** — 原因为空、不能停用自己、或停用后系统无启用管理员

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员或 CSRF 校验失败

**404 Not Found** — 用户不存在

#### 业务规则

- 不能停用自己
- 如果目标是启用管理员，停用后必须至少保留一个启用管理员
- 实际停用时写 audit_log（actionType: USER_DEACTIVATED）
- 实际停用后使目标用户所有 Session 失效

---

### PUT /api/admin/users/{id}/restore

管理员恢复已停用用户。恢复后用户必须重新登录，旧 Session 不恢复。

#### 认证

需要管理员（ROLE_ADMIN）角色。

#### CSRF

需要携带有效 CSRF Token。

#### 成功响应

**204 No Content**

#### 幂等

- 已启用用户重复恢复返回 204，不重复更新，不重复写 audit_log

#### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员或 CSRF 校验失败

**404 Not Found** — 用户不存在

#### 业务规则

- 恢复后不创建或恢复旧 Session
- 用户必须重新登录
- 实际恢复时写 audit_log（actionType: USER_RESTORED）

---

### PUT /api/admin/users/{id}/grant-admin

授予普通用户管理员权限。目标用户需重新登录以获取 ROLE_ADMIN。

#### 认证

需要管理员（ROLE_ADMIN）角色。

#### CSRF

需要携带有效 CSRF Token。

#### 成功响应

**204 No Content**

#### 幂等

- 已是管理员时重复授予返回 204，不重复更新，不重复写 audit_log

#### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员或 CSRF 校验失败

**404 Not Found** — 用户不存在

#### 业务规则

- 实际变化时写 audit_log（actionType: ADMIN_GRANTED）
- 使目标用户全部 Session 失效

---

### PUT /api/admin/users/{id}/revoke-admin

撤销用户的管理员权限。目标用户所有现有 Session 立即失效。

#### 认证

需要管理员（ROLE_ADMIN）角色。

#### CSRF

需要携带有效 CSRF Token。

#### 成功响应

**204 No Content**

#### 幂等

- 已是普通用户时重复撤销返回 204，不重复更新，不重复写 audit_log

#### 错误响应

**400 Bad Request** — 不能撤销自己，或撤销后系统无启用管理员

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员或 CSRF 校验失败

**404 Not Found** — 用户不存在

#### 业务规则

- 不能撤销自己的管理员权限
- 撤销后必须至少保留一个启用管理员
- 实际变化时写 audit_log（actionType: ADMIN_REVOKED）
- 使目标用户全部 Session 失效

---

## 训练计划 API

### GET /api/training-plans

分页查询训练计划列表。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| type | string | PUBLIC | 计划类型：PUBLIC / PERSONAL |
| timeStatus | string | — | 时间状态：NOT_STARTED / ONGOING / ENDED |
| keyword | string | — | 关键词，匹配标题 |
| page | int | 1 | 页码，最小 1 |
| size | int | 20 | 每页数量，1-100 |

#### 成功响应

**200 OK**

```json
{
  "plans": [
    {
      "id": 1,
      "title": "2026 暑期集训",
      "planType": "PUBLIC",
      "timeStatus": "ONGOING",
      "isActive": true,
      "creatorUserId": 1,
      "creatorNickname": "Admin",
      "problemCount": 10,
      "memberCount": 5,
      "myProgress": {"completed": 3, "total": 10},
      "startTime": "2026-07-01T00:00:00",
      "endTime": "2026-08-31T23:59:59",
      "createTime": "2026-07-01T00:00:00",
      "updateTime": "2026-07-15T12:00:00"
    }
  ],
  "total": 5,
  "page": 1,
  "size": 20
}
```

---

### POST /api/training-plans

创建训练计划。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 校验规则 |
|------|------|------|----------|
| title | string | 是 | 非空，最大 128 字符 |
| description | string | 否 | 最大 5000 字符 |
| planType | string | 否 | PERSONAL（默认）/ PUBLIC，仅管理员可创建 PUBLIC |
| startTime | datetime | 否 | ISO 8601 格式 |
| endTime | datetime | 否 | ISO 8601 格式 |
| problemIds | number[] | 否 | 题目 ID 列表，按顺序加入计划。仅 ACTIVE 题目可加入，重复 ID 返回 400 |

#### 成功响应

**200 OK** — 返回 PlanDetailResponse，包含题目列表和成员列表。

#### 错误响应

**400 Bad Request** — 字段校验失败

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或非管理员尝试创建 PUBLIC 计划

---

### GET /api/training-plans/{id}

获取训练计划详情，包含题目列表、成员列表和当前用户进度。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### 可见性

- PUBLIC 计划：所有已登录用户可见
- PERSONAL 计划：仅创建者和管理员可见
- 停用计划：已有参与者和管理员可见

#### 成功响应

**200 OK**

```json
{
  "id": 1,
  "title": "2026 暑期集训",
  "description": "暑期专项训练",
  "planType": "PUBLIC",
  "timeStatus": "ONGOING",
  "isActive": true,
  "creatorUserId": 1,
  "creatorNickname": "Admin",
  "startTime": "2026-07-01T00:00:00",
  "endTime": "2026-08-31T23:59:59",
  "problems": [
    {
      "problemId": 1,
      "title": "Two Sum",
      "platform": "CUSTOM",
      "difficulty": "800",
      "sortOrder": 0,
      "active": true,
      "required": true,
      "myStatus": "ACCEPTED",
      "performanceNote": "需要复习"
    }
  ],
  "members": [
    {
      "userId": 2,
      "nickname": "Bob",
      "completedCount": 3,
      "totalCount": 10,
      "requiredCompletedCount": 2,
      "requiredTotal": 5,
      "currentLastAcceptedTime": "2026-07-28T14:30:00",
      "deadlineLastAcceptedTime": "2026-07-28T14:30:00",
      "currentCompletedAt": null,
      "deadlineCompletedAt": null,
      "deadlineCompletedCount": 2,
      "rank": 1,
      "completionOrder": null,
      "joinTime": "2026-07-05T10:00:00",
      "creator": false
    }
  ],
  "myProgress": {
    "requiredCompletedCount": 2,
    "requiredTotal": 5,
    "optionalCompletedCount": 1,
    "optionalTotal": 5
  },
  "createTime": "2026-07-01T00:00:00",
  "updateTime": "2026-07-15T12:00:00"
}
```

#### 错误响应

**401 Unauthorized** — 未登录

**404 Not Found** — 计划不存在或无权查看

---

### PUT /api/training-plans/{id}

更新训练计划标题、说明和时间。仅创建者或管理员可操作。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 最大 128 字符 |
| description | string | 否 | 最大 5000 字符 |
| startTime | datetime | 否 | ISO 8601 格式 |
| endTime | datetime | 否 | ISO 8601 格式 |

#### 成功响应

**200 OK** — 返回更新后的 PlanDetailResponse。

#### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或无管理权限

**404 Not Found** — 计划不存在

---

### PUT /api/training-plans/{id}/deactivate

停用训练计划。仅创建者或管理员可操作。

#### 请求体（可选）

```json
{"reason": "停用原因，最长 500 字符"}
```

#### 成功响应

**204 No Content**

---

### PUT /api/training-plans/{id}/restore

恢复训练计划。仅创建者或管理员可操作。

#### 成功响应

**204 No Content**

---

### POST /api/training-plans/{id}/problems

向训练计划添加题目。仅创建者或管理员可操作。

#### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| problemId | long | 是 | 题目 ID |
| sortOrder | int | 否 | 排序位置，默认追加到末尾 |

#### 成功响应

**204 No Content**

#### 错误响应

**400 Bad Request** — 题目不存在或已停用

**409 Conflict** — 题目已在计划中

---

### PUT /api/training-plans/{id}/problems

批量更新训练计划题目列表（替换整个列表）。仅创建者可操作。会对比新旧列表，无变更时跳过通知。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### 权限

仅计划创建者可操作。非创建者（包括管理员）返回 403。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| problems | object[] | 否 | 题目列表（空数组清除所有题目） |
| problems[].problemId | long | 是 | 题目 ID |
| problems[].sortOrder | int | 否 | 排序位置 |

#### 业务规则

- 仅 ACTIVE（status=1）题目可加入，停用题目返回 400
- 重复 problemId 返回 400
- 不存在的题目返回 404
- 新旧列表完全相同时跳过实际更新（不触发通知）
- PUBLIC 计划题目变更触发 TRAINING_PROBLEMS_CHANGED 通知
- 所有操作在一个事务中完成

#### 成功响应

**204 No Content**

#### 错误响应

**400 Bad Request** — 题目不存在、已停用、或 ID 重复

**401 Unauthorized** — 未登录

**403 Forbidden** — 非计划创建者

**404 Not Found** — 计划不存在

**409 Conflict** — 题目重复

---

### DELETE /api/training-plans/{id}/problems/{problemId}

从训练计划移除题目。仅创建者或管理员可操作。

#### 成功响应

**204 No Content**

---

### POST /api/training-plans/{id}/members/me

加入公开训练计划。

#### 成功响应

**204 No Content**

#### 错误响应

**400 Bad Request** — 计划已停用或已结束（不接受新成员）

**409 Conflict** — 已是计划成员

---

### DELETE /api/training-plans/{id}/members/{userId}

从训练计划移除成员。仅创建者或管理员可操作。

#### 成功响应

**204 No Content**

---

### PUT /api/training-plans/{planId}/problems/{problemId}/status

更新成员在训练计划中的题目完成状态。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|------|------|------|----------|------|
| status | string | 是 | `^(NOT_STARTED\|CHALLENGING)$` | 题目状态，ACCEPTED 不可手动设置 |

#### 状态机

| 当前状态 | 允许操作 | 说明 |
|----------|----------|------|
| NOT_STARTED | → CHALLENGING | 用户手动标记挑战 |
| CHALLENGING | → NOT_STARTED | 用户手动取消挑战 |
| NOT_STARTED / CHALLENGING | → ACCEPTED | 仅可信 OJ 同步自动设置 |
| ACCEPTED | 不可修改 | 永久锁定 |

#### 业务规则

- 仅激活中的计划可操作，已停用计划返回 400
- 仅计划成员可操作，非成员返回 403
- 题目必须属于该计划，否则返回 404
- ACCEPTED 作为请求值直接返回 400 校验失败
- 同状态重复提交幂等（不触发更新）

#### 成功响应

**204 No Content**

#### 错误响应

**400 Bad Request** — 计划已停用或状态值无效

**401 Unauthorized** — 未登录

**403 Forbidden** — 非计划成员

**404 Not Found** — 计划或题目不存在

---

### PUT /api/training-plans/{planId}/problems/{problemId}/note

更新成员在训练计划题目上的备注。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| note | string | 否 | 备注内容，最长 500 字符 |

#### 成功响应

**204 No Content**

#### 错误响应

**400 Bad Request** — 备注超过 500 字符

**401 Unauthorized** — 未登录

**403 Forbidden** — 非计划成员

**404 Not Found** — 计划或题目不存在

---

### GET /api/training-plans/{planId}/members/{userId}/progress

获取指定成员在训练计划中的详细进度，包含题目列表和完成状态。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### 可见性

- PUBLIC 计划：计划成员和管理员可查看其他成员进度
- PERSONAL 计划：仅创建者可查看

#### 成功响应

**200 OK**

```json
{
  "userId": 2,
  "nickname": "Bob",
  "completedCount": 3,
  "totalCount": 10,
  "lastAcceptedTime": "2026-07-28T14:30:00",
  "rank": 1,
  "completionOrder": 1,
  "problems": [
    {
      "problemId": 1,
      "problemTitle": "Two Sum",
      "platform": "CUSTOM",
      "difficulty": "800",
      "problemActive": true,
      "sortOrder": 0,
      "required": true,
      "myStatus": "ACCEPTED",
      "performanceNote": null
    }
  ]
}
```

#### 错误响应

**401 Unauthorized** — 未登录

**404 Not Found** — 计划不存在、成员不存在或无权查看

---

### 计划中/尚未实现的训练计划 API（草稿）

以下接口为后续版本规划，当前未实现：

| 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|
| PUT | `/api/training-plans/{id}/problems` | 批量更新题目列表（含排序） | 已实现 |
| GET | `/api/training-plans/{id}/members` | 分页查询成员列表及进度 | 计划中/尚未实现 |
| GET | `/api/training-plans/{id}/statistics` | 训练计划统计（完成率、排名快照） | 计划中/尚未实现 |
| GET | `/api/training-plans/{id}/rankings` | 赛时排名快照 | 计划中/尚未实现 |

当前创建和更新训练计划不支持通过 `problemIds` 字段批量设置题目。题目只能通过 `POST /{id}/problems` 逐条添加和 `DELETE /{id}/problems/{problemId}` 逐条移除。计划类型（PERSONAL/PUBLIC）创建后不可更改。

---

## GET /api/notifications

分页查询当前用户的站内通知。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

GET 请求不需要 CSRF Token。

### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，最小 1 |
| size | int | 20 | 每页数量，1-100 |
| unreadOnly | boolean | false | 仅返回未读通知 |

### 成功响应

**200 OK**

```json
{
  "items": [
    {
      "id": 1,
      "notificationType": "POST_COMMENTED",
      "actorUserId": 2,
      "resourceType": "POST",
      "resourceId": 10,
      "payload": {
        "postTitle": "Test Post",
        "actorNickname": "Alice"
      },
      "isRead": false,
      "readTime": null,
      "createTime": "2026-07-28T12:00:00"
    }
  ],
  "total": 25,
  "page": 1,
  "size": 20
}
```

### 错误响应

**401 Unauthorized** — 未登录

**400 Bad Request** — page < 1 或 size 不在 1-100 范围内

---

## GET /api/notifications/unread-count

获取当前用户未读通知数量。

### 认证

需要登录后携带 JSESSIONID Cookie。

### 成功响应

**200 OK**

```json
{"count": 5}
```

### 错误响应

**401 Unauthorized** — 未登录

---

## PUT /api/notifications/{id}/read

标记单条通知为已读。仅通知接收者可操作。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 权限

- 仅通知的 `recipientUserId` 匹配当前用户可操作
- 操作他人通知返回 403

### 幂等

已读通知重复标记无副作用，不重复更新 `readTime`。

### 成功响应

**204 No Content**

### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或通知不属于当前用户

**404 Not Found** — 通知不存在

---

## PUT /api/notifications/read-all

将当前用户所有未读通知标记为已读。幂等操作。

### 认证

需要登录后携带 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。

### 成功响应

**204 No Content**

### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token

---

## 通知类型与 Payload 结构

### POST_COMMENTED — 帖子被评论

```json
{"postTitle": "...", "actorNickname": "..."}
```

### COMMENT_REPLIED — 评论被回复

```json
{"postTitle": "...", "actorNickname": "..."}
```

### POST_ADMIN_DEACTIVATED — 管理员停用帖子

```json
{"postTitle": "...", "reason": "..."}
```

### COMMENT_ADMIN_DEACTIVATED — 管理员停用评论

```json
{"reason": "..."}
```

### POST_RESTORED — 管理员恢复帖子

```json
{"postTitle": "..."}
```

### COMMENT_RESTORED — 管理员恢复评论

```json
{}
```

### TRAINING_MEMBER_REMOVED — 被移出训练计划

```json
{"planTitle": "..."}
```

### TRAINING_ADMIN_DEACTIVATED — 管理员停用训练计划

```json
{"planTitle": "...", "reason": "..."}
```

### TRAINING_RESTORED — 管理员恢复训练计划

```json
{"planTitle": "..."}
```

### TRAINING_SCHEDULE_CHANGED — 训练计划时间更新

```json
{"planTitle": "..."}
```

### TRAINING_PROBLEMS_CHANGED — 训练计划题目更新

```json
{"planTitle": "..."}
```

### 实现说明

- 自操作不产生通知（actorUserId == recipientUserId 时跳过）
- 持久化失败只记日志，不抛出异常（best-effort）
- 批量发送每批 200 条 chunk 插入
- 事件通过 `@TransactionalEventListener(phase = AFTER_COMMIT)` 投递

---

## OJ 账号 API

### GET /api/oj-accounts/me

获取当前用户的 OJ 账号绑定状态。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### 成功响应

**200 OK**

```json
{
  "hasAccount": true,
  "id": 1,
  "platform": "CODEFORCES",
  "externalUserId": "tourist",
  "displayName": "tourist",
  "verifyStatus": 1,
  "syncEnabled": 1,
  "lastSyncTime": "2026-07-28T12:00:00",
  "lastSyncSuccess": 1
}
```

未绑定时返回 `{"hasAccount": false}`。

---

### POST /api/oj-accounts

绑定 Codeforces 账号。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 请求字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| handle | string | 是 | Codeforces 用户名，去除首尾空格 |

#### 成功响应

**204 No Content**

#### 错误响应

**400 Bad Request** — handle 为空

**409 Conflict** — 已绑定过账号，或该 handle 已被其他用户绑定

---

### DELETE /api/oj-accounts/me

解绑当前用户的 OJ 账号。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 成功响应

**204 No Content**

---

### GET /api/oj-accounts/admin

管理员查看待审核的 OJ 账号列表（verifyStatus=0）。

#### 认证

需要管理员角色。

#### 成功响应

**200 OK**

```json
[
  {
    "id": 1,
    "userId": 5,
    "platform": "CODEFORCES",
    "externalUserId": "new_user",
    "displayName": "new_user",
    "verifyStatus": 0,
    "syncEnabled": 1,
    "lastSyncTime": null
  }
]
```

#### 错误响应

**403 Forbidden** — 非管理员

---

### POST /api/oj-accounts/admin/{id}/verify

管理员审核 OJ 账号。

#### 认证

需要管理员角色。

#### CSRF

需要携带有效 CSRF Token。

#### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| status | int | 1 | 1=通过, 2=拒绝 |

#### 成功响应

**204 No Content**

#### 错误响应

**403 Forbidden** — 非管理员
**404 Not Found** — 账号不存在

---

### POST /api/oj-accounts/me/sync

同步当前用户已验证 Codeforces 账号的提交记录。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### CSRF

需要携带有效 CSRF Token。

#### 前置条件

- 已绑定 Codeforces 账号（`platform=CODEFORCES`）
- 账号审核状态为 VERIFIED（`verifyStatus=1`）

#### 同步语义

- 从 Codeforces API 获取最多 500 条最新提交
- 使用 `last_sync_cursor`（最大 `remote_submission_id`）做增量同步
- 已同步的提交自动跳过（游标比对）
- 提交记录通过 `uk_platform_submission (platform, remote_submission_id)` 保证幂等
- 首次 AC 通过 `oj_first_ac` 表的 `uk_user_platform_problem (user_id, platform, external_problem_key)` 保证原子唯一

#### AC 判定规则

- 仅 `verdict=OK` 计为 AC
- 同一用户同一题多次 OK 只计一次 AC
- 题目标识：`contestId + index`（如 `123A`），无 contestId 时使用 `problemsetName + index`
- 首次 AC 写入 `oj_first_ac` 表，DB 唯一约束做并发兜底

#### 成功响应

**200 OK**

```json
{
  "accountId": 1,
  "handle": "tourist",
  "fetchedCount": 100,
  "insertedCount": 10,
  "acceptedCount": 5,
  "newAcceptedProblemCount": 2,
  "lastSyncTime": "2026-07-28T12:00:00",
  "syncStatus": "SUCCESS"
}
```

#### 错误响应

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token

**404 Not Found** — 未绑定 Codeforces 账号，或 CF 账号不存在

**409 Conflict** — 账号审核未通过

**429 Too Many Requests** — Codeforces API 频率限制

**502 Bad Gateway** — Codeforces API 返回异常数据

**503 Service Unavailable** — Codeforces 服务不可达

#### 幂等保证

- 重复同步不产生重复提交（`uk_platform_submission` 唯一约束兜底）
- 重复同步 `insertedCount=0`
- 游标仅在实际同步成功后推进
- 同步失败时旧提交和 AC 数据完整保留

---

## 排行榜 API

### GET /api/leaderboard

分页查询可信排行榜。

#### 认证

需要登录后携带 JSESSIONID Cookie。

#### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| period | string | total | 时间范围：`total`（总榜）、`7d`（近7天）、`30d`（近30天） |
| page | int | 1 | 页码，最小 1 |
| size | int | 20 | 每页数量，1-100（超出范围重置为 20） |

#### 数据口径

- 统计来源：`oj_first_ac` 表（每行 = 一个唯一 AC）
- 通过 `submission_id` 关联 `oj_submission` 获取 `submitted_time` 用于 7d/30d 过滤
- 仅统计 VERIFIED 账号（`oj_account.verify_status = 1`）
- 禁用用户（`app_user.status = 0`）不在结果中
- 不要求 `problem_id` 非空；未映射到本地题库的 AC 仍可计入
- 一名用户同一平台同一题只计一次（`oj_first_ac.uk_user_platform_problem` 唯一约束）
- 7d/30d 基于 `oj_submission.submitted_time` 过滤，ALL 不限制时间

#### 排序规则

- 主排序：`solved_count DESC`（唯一通过题数降序）
- 次排序：`user_id ASC`（稳定兜底）
- 排名字段 `rank` 从 `(page - 1) * size + 1` 开始递增，SQL 已排除的禁用用户不会产生排名空缺

#### 成功响应

**200 OK**

```json
{
  "entries": [
    {
      "rank": 1,
      "userId": 5,
      "username": "alice",
      "nickname": "Alice",
      "avatarUrl": null,
      "solvedCount": 42,
      "isMe": false
    },
    {
      "rank": 2,
      "userId": 3,
      "username": "bob",
      "nickname": "Bob",
      "avatarUrl": "https://example.com/avatar.png",
      "solvedCount": 30,
      "isMe": true
    }
  ],
  "total": 15,
  "page": 1,
  "size": 20
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| entries | array | 排行榜条目列表 |
| entries[].rank | int | 排名 |
| entries[].userId | long | 用户 ID |
| entries[].username | string | 用户名 |
| entries[].nickname | string | 用户昵称 |
| entries[].avatarUrl | string\|null | 头像 URL |
| entries[].solvedCount | int | 唯一通过题数 |
| entries[].isMe | boolean | 是否为当前登录用户 |
| total | long | 参与排行榜的用户总数（仅 VERIFIED 账号+非禁用用户） |
| page | int | 当前页码 |
| size | int | 每页数量 |

#### 错误响应

**400 Bad Request** — page < 1（服务端自动修正为 1，不返回 400）

**401 Unauthorized** — 未登录

**500 Internal Server Error** — 数据库或服务异常

#### 空数据

排行榜无数据时（无 VERIFIED 账号或无人有 AC 记录）返回空数组：

```json
{"entries": [], "total": 0, "page": 1, "size": 20}
```

---

## 操作日志 API

### GET /api/admin/audit-logs

管理员分页查询操作日志。

#### 认证

需要管理员（ROLE_ADMIN）角色。

#### CSRF

GET 请求不需要 CSRF Token。

#### 查询参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，最小 1，超出钳位 |
| size | int | 20 | 每页数量，1-100，超出钳位 |
| actionType | string | — | 操作类型筛选。可选值：USER_DEACTIVATED, USER_RESTORED, ADMIN_GRANTED, ADMIN_REVOKED, POST_ADMIN_DEACTIVATED, POST_RESTORED, COMMENT_ADMIN_DEACTIVATED, COMMENT_RESTORED, TRAINING_ADMIN_DEACTIVATED, TRAINING_RESTORED, PROBLEM_ADMIN_DEACTIVATED, PROBLEM_RESTORED, OJ_ACCOUNT_VERIFIED, OJ_ACCOUNT_REJECTED |
| targetType | string | — | 目标类型筛选。可选值：USER, POST, COMMENT, TRAINING_PLAN, PROBLEM, OJ_ACCOUNT |
| targetId | long | — | 目标资源 ID 精确匹配 |
| actorKeyword | string | — | 操作者关键词，匹配 username 或 nickname（模糊搜索） |
| startTime | string | — | 开始时间（含），ISO 8601 格式，如 `2026-07-01T00:00:00` |
| endTime | string | — | 结束时间（含），ISO 8601 格式，如 `2026-07-29T23:59:59` |

#### 成功响应

**200 OK**

```json
{
  "items": [
    {
      "id": 1,
      "actionType": "USER_DEACTIVATED",
      "actorUserId": 1,
      "actorUsername": "admin",
      "actorNickname": "Admin",
      "targetType": "USER",
      "targetId": 2,
      "beforeState": "ACTIVE",
      "afterState": "DEACTIVATED",
      "reason": "违规行为",
      "createTime": "2026-07-29T12:00:00"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

#### 错误响应

**400 Bad Request** — actionType 或 targetType 无效

**401 Unauthorized** — 未登录

**403 Forbidden** — 非管理员

#### 过滤规则

- actionType/targetType 白名单校验，无效值返回 400
- actorKeyword 匹配操作者的 username 或 nickname（SQL LIKE + 内存过滤）
- startTime/endTime 过滤 create_time 范围
- targetId 精确匹配 resource_id
- 按 create_time DESC, id DESC 稳定排序

#### 实现说明

- 操作者信息（username、nickname）通过 selectBatchIds 批量加载，避免 N+1
- actorKeyword 在批量加载后内存过滤（SQL 端只过滤 audit_log 行）
- 响应不包含 passwordHash、email 等敏感字段
- 所有高风险操作写日志时使用 AuditLogConstants 中的标准化 actionType

#### 已集成的写日志操作

| 操作 | actionType | 模块 |
|------|-----------|------|
| 管理员停用用户 | USER_DEACTIVATED | AdminUserService |
| 管理员恢复用户 | USER_RESTORED | AdminUserService |
| 授予管理员 | ADMIN_GRANTED | AdminUserService |
| 撤销管理员 | ADMIN_REVOKED | AdminUserService |
| 管理员停用帖子 | POST_ADMIN_DEACTIVATED | AdminContentService |
| 管理员恢复帖子 | POST_RESTORED | AdminContentService |
| 管理员停用评论 | COMMENT_ADMIN_DEACTIVATED | AdminContentService |
| 管理员恢复评论 | COMMENT_RESTORED | AdminContentService |
| 管理员停用训练计划 | TRAINING_ADMIN_DEACTIVATED | TrainingPlanService |
| 管理员恢复训练计划 | TRAINING_RESTORED | TrainingPlanService |
| 管理员强制停用题目 | PROBLEM_ADMIN_DEACTIVATED | ProblemCommandService |
| 管理员恢复题目 | PROBLEM_RESTORED | ProblemCommandService |
| OJ 账号审核通过 | OJ_ACCOUNT_VERIFIED | OjAccountService |
| OJ 账号审核拒绝 | OJ_ACCOUNT_REJECTED | OjAccountService |
