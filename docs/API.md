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

**409 Conflict** — 用户名或邮箱已存在

```json
{
  "code": 409,
  "message": "用户名已被使用",
  "timestamp": "2026-07-24T19:28:49.1197739"
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

登录 POST 不需要 CSRF Token。

### 认证流程

1. UsernamePasswordAuthenticationToken 构建
2. DaoAuthenticationProvider 通过 DatabaseUserDetailsService 加载用户
3. BCryptPasswordEncoder 验证密码
4. 认证成功后 SecurityContext 写入 HttpSession
5. ChangeSessionIdAuthenticationStrategy 更换 Session ID（防 Session Fixation）
6. 返回用户信息

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

## GET /api/problems/{id}

获取题目详情。

### 认证

需要登录后携带 JSESSIONID Cookie。

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

**404 Not Found** — 题目不存在或已禁用（不区分，统一返回"题目不存在"）

```json
{"code": 404, "message": "题目不存在", "timestamp": "..."}
```

### 实现说明

- 仅返回 status=1 的题目，已禁用题目对普通用户不可见
- 不暴露 status 字段

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

示例：`?page=1&size=20&platform=CODEFORCES&difficulty=1200&keyword=two`

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

由题目创建者或管理员完整修改题目。普通用户只能修改自己创建的题目，管理员可以修改任意正常状态的题目。

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
- `status` — 不可通过此接口修改
- `id` / `createTime` / `updateTime` — 由数据库维护

### 规范化规则

与 POST /api/problems 完全一致。所有字符串字段经过 ProblemFieldNormalizer 统一处理。

### 所有权校验

- 普通用户只能修改 `creatorUserId == operatorUserId` 的题目
- 管理员可以修改任意正常状态的题目
- 不满足则返回 403

### 成功响应

**200 OK** — 返回更新后从数据库重新读取的题目详情（含最新 updateTime）

### 错误响应

**400 Bad Request** — 字段校验失败或非 CUSTOM 平台未提供题目标识

**401 Unauthorized** — 未登录

**403 Forbidden** — 缺少/无效 CSRF Token，或无资源管理权限

**404 Not Found** — 题目不存在或已禁用（不区分）

**409 Conflict** — 相同平台+题目标识已被其他题目使用（排除自身）

### 实现说明

- 使用 LambdaUpdateWrapper.set() 显式设置字段，确保 null 值可写入
- UPDATE WHERE 包含 status=1，防止并发禁用情况下修改已停用题目
- 题目标识查重时排除自身（.ne(Problem::getId, problemId)）
- 更新后重新查询确保响应反映数据库实际状态（含 ON UPDATE CURRENT_TIMESTAMP）
- @Transactional 保证查询→校验→更新→回读的读写边界一致
- 权限判断在 Service 层完成，SecurityFilterChain 仅要求 authenticated()
