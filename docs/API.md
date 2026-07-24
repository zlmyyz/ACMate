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
