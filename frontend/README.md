# ACMate Frontend

社团程序设计竞赛训练与题目管理平台前端。

## 技术栈

- Vue 3 + TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
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
- CSRF Token 动态获取（登出等写操作前通过 GET /api/auth/csrf 获取）
- 路由守卫：未登录跳转登录页，已登录跳转首页
- 不读取、保存或打印 JSESSIONID
