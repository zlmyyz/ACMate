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

## POST /api/auth/logout

退出登录，使当前 Session 失效。

### 认证

需要携带登录时获得的 JSESSIONID Cookie。

### CSRF

需要携带有效 CSRF Token。不在 CSRF 忽略列表中。

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
