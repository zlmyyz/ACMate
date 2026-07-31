# ACMate

校内程序设计竞赛社的训练与题解交流平台。支持帖子讨论、题库管理、训练计划跟踪、Codeforces 账号同步与可信排行榜。

功能演示

https://github.com/user-attachments/assets/e4facc2c-1c83-44a4-98ed-668b53096a94

## 技术栈

### 后端
- **Java 17** / Spring Boot 4.1.0
- **MyBatis-Plus 3.5.17** + MySQL 8.0
- **Spring Security** — Session 登录 + CSRF
- **Flyway** — 数据库版本迁移
- **Maven** — 构建管理

### 前端
- **Vue 3** + TypeScript + Vite 8
- **Element Plus** — UI 组件库
- **Pinia** — 状态管理
- **Vue Router 5** — 路由
- **Axios** — HTTP 请求

## 功能概览

### 用户系统
- 注册 / Session 登录 / 退出
- 普通用户与管理员两级权限
- 个人资料编辑（昵称、头像、Bio）
- 公开个人主页

### 帖子讨论
- 帖子 CRUD（普通帖 / 求助帖 / 公告帖）
- 一级评论与嵌套回复
- 点赞 / 取消点赞
- 软删除与状态管理
- HELP 帖子采纳评论

### 题库系统
- 题目 CRUD（标题、难度、标签、题解）
- 题目停用 / 恢复

### 训练计划
- 训练计划 CRUD
- 批量选题、排序、批量更新
- 成员加入 / 退出 / 重新加入
- 成员进度跟踪（题目状态、备注、排名、计分）
- 必做 / 选做题目区分
- 计划截止时间与停用

### OJ 账号绑定与同步
- Codeforces 账号绑定
- 牛客账号绑定（仅记录）
- Codeforces 提交增量同步（cursor 分页 + first-AC 原子约束 + 幂等）
- 每小时整点自动同步
- 同步任务日志

### 可信排行榜
- 全站 / 30 天 / 7 天排行
- 并列排名、分页、lastAcceptedTime
- 基于真实 Codeforces 数据

### 通知系统
- 11 种站内事件通知（评论、回复、点赞、采纳、计划邀请等）
- 轮询 + Pinia store + 未读过滤

### 管理员功能
- 用户管理（列表、筛选、停用、恢复、授予 / 撤销管理员）
- 操作日志（14 种 actionType + 7 种 targetType，完整过滤）
- Session 失效

## 项目结构

```
ACMate/
├── src/main/java/com/itnoduck/acmate/
│   ├── admin/           # 管理员功能
│   ├── auditlog/        # 操作日志
│   ├── common/          # 公共工具类
│   ├── config/          # Spring 配置
│   ├── discussion/      # 帖子、评论、回复、点赞
│   ├── export/          # 数据导出
│   ├── leaderboard/     # 排行榜
│   ├── notification/    # 站内通知
│   ├── oj/              # OJ 账号与提交同步
│   ├── problem/         # 题库
│   ├── security/        # 认证与授权
│   ├── synctask/        # 同步任务日志
│   ├── training/        # 训练计划
│   └── user/            # 用户
├── src/main/resources/
│   └── db/migration/    # Flyway 迁移脚本（V1-V15）
├── src/test/            # 后端测试（541 pass）
├── frontend/
│   ├── src/api/         # API 封装
│   ├── src/components/  # 通用组件
│   ├── src/composables/ # 组合式函数
│   ├── src/constants/   # 常量
│   ├── src/router/      # 路由配置
│   ├── src/stores/      # Pinia stores
│   ├── src/types/       # TypeScript 类型
│   ├── src/views/       # 页面组件
│   └── src/__tests__/   # 前端测试（197 pass）
└── docs/                # 开发文档
```

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Node.js 22+（前端开发）

### 1. 创建数据库

```bash
mysql -u root -p < src/main/resources/db/manual/00_create_database.sql
```

Flyway 会在应用启动时自动执行所有迁移，无需手动导入。

### 2. 配置环境变量

```bash
export DB_PASSWORD="your_password"
# 可选：DB_HOST、DB_PORT、DB_NAME、DB_USERNAME
```

### 3. 启动后端

```bash
./mvnw -DskipTests spring-boot:run
```

### 4. 启动前端（可选）

```bash
cd frontend
npm install
npm run dev
```

### 健康检查

```bash
curl http://localhost:8080/api/health
# {"status":"UP","database":"UP","userCount":5}
```

## 开发进度

详见 [ROADMAP.md](docs/ROADMAP.md)。



