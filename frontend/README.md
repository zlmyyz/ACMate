# ACMate Frontend

社团程序设计竞赛训练与题目管理平台前端。

## 技术栈

- Vue 3 + TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- markdown-it（Markdown 渲染，HTML 已禁用）
- Vitest

## 开发

```bash
npm install
npm run dev         # http://localhost:5173
npm run build       # 生产构建
npm run test:unit   # 单元测试
npm run type-check  # 类型检查
npm run lint        # 代码检查
```

## 后端代理

Vite 将 `/api` 代理到 `http://localhost:8080`（后端 Spring Boot 默认端口）。

## 设计参考

Stitch 设计资源位于 `design/` 目录，包含 16 个页面的截图和 HTML 导出。
详见 `design/README.md`。

## 认证

- Session Cookie 认证（withCredentials）
- CSRF Token 动态获取（写操作前通过 GET /api/auth/csrf 获取）
- 路由守卫：未登录跳转登录页，已登录跳转首页
- 不读取、保存或打印 JSESSIONID
- 网络异常/500 不静默视为未登录，提供重试入口

## 已实现页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `/` | 欢迎信息、快捷入口 |
| 登录 | `/login` | 用户名密码登录 |
| 注册 | `/register` | 新用户注册 |
| 题库 | `/problems` | 题目分页列表，支持关键词/平台/难度筛选 |
| 题目详情 | `/problems/:id` | 详情展示，创建者/管理员可编辑 |
| 创建题目 | `/problems/create` | 表单创建，含草稿保存 |
| 编辑题目 | `/problems/:id/edit` | 复用创建表单，含离开确认 |
| 403 | `/403` | 无权限页面 |
| 404 | `/404` | 页面不存在 |
