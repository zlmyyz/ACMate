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
