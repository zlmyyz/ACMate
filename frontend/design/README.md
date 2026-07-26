# ACMate 设计资源目录

## Stitch 项目信息

- **Project Title**: UI Prototype Generator
- **Project ID**: 7282810004874907230
- **下载日期**: 2026-07-26

## 页面清单

| 页面 | Screen ID | 截图 | HTML | 状态 |
|------|-----------|------|------|------|
| 首页 | ff99453aefc24532aa7f58a02550e383 | screenshots/home/home.png | exports/home/home.html | 已实现 |
| 公共题库 | b1b7bae40aad47419caec55dbfe4526d | screenshots/problems/problems.png | exports/problems/problems.html | 后续 |
| 题目详情 | d98d28b9ca2d44cdb92113caeff20c7e | screenshots/problem-detail/problem-detail.png | exports/problem-detail/problem-detail.html | 后续 |
| 创建题目 | 1b9d3281772943d0bf345585a7fb93fb | screenshots/create-problem/create-problem.png | exports/create-problem/create-problem.html | 后续 |
| 我的题目 | d5dc184081d140378a31468d96bc2bfa | screenshots/my-problems/my-problems.png | exports/my-problems/my-problems.html | 后续 |
| 管理员全部题库 | e0470ad63f5743bdb1e4ed80701f50ae | screenshots/admin-problems/admin-problems.png | exports/admin-problems/admin-problems.html | 后续 |
| 用户主页 | 54c4dc41c21045988457f79ea5b0b992 | screenshots/user-profile/user-profile.png | exports/user-profile/user-profile.html | 后续 |
| 训练计划列表 | ab45b5f8fc0340e3b2040e63ff2f4a3a | screenshots/training-plans/training-plans.png | exports/training-plans/training-plans.html | 仅参考 |
| 训练计划详情 | aeac79f819b94530af3a682f314df0ed | screenshots/training-plan-detail/training-plan-detail.png | exports/training-plan-detail/training-plan-detail.html | 仅参考 |
| 创建训练计划 | 6edf9562428d4f9bbe60e2f5c8cb78fb | screenshots/create-training-plan/create-training-plan.png | exports/create-training-plan/create-training-plan.html | 仅参考 |
| 成员进度统计 | 1b105311cd024a60bbfb76f1e6312f67 | screenshots/member-progress/member-progress.png | exports/member-progress/member-progress.html | 仅参考 |
| OJ 账号管理 | 51d85b13f42c4103b197ceb006635c1e | screenshots/oj-account/oj-account.png | exports/oj-account/oj-account.html | 仅参考 |
| 排行榜 | b849f3dba975474b802c9759bf1b160d | screenshots/leaderboard/leaderboard.png | exports/leaderboard/leaderboard.html | 仅参考 |
| 帖子详情 | 5e09ef0def5d489fb3a32dd4734dd689 | screenshots/post-detail/post-detail.png | exports/post-detail/post-detail.html | 仅参考 |
| 讨论区 | b140c6529df740b28e01941ef10c18cb | screenshots/discussions/discussions.png | exports/discussions/discussions.html | 仅参考 |
| PRD 文档 | 2975163955636462851 | — | exports/prd/prd.html | 参考 |

## Design System

Design System 信息包含在每个页面的 `exports/*/*-theme.json` 中。
各页面共享相同的主题配置（颜色、字体、间距等），theme JSON 内容一致。

单独 Design System（`asset-stub-assets_f450921adb3342e5b5bd619d8bfe493e`）无独立下载 URL，
其内容已内联在每个 Screen 的 `designSystem` 字段中。

## 静态资源

Stitch 未提供可复用的本地图片资源。页面中的人像和图标使用 Google 远程占位 URL。
- `assets/` 目录当前为空
- 图标使用 Google Material Symbols Outlined（字体图标，非本地文件）

## 技术栈

- 纯静态 HTML + Tailwind CSS CDN
- Google Fonts: Hanken Grotesk, Inter, JetBrains Mono
- Material Symbols Outlined 字体图标
- 无 React/Next.js/Vue 框架依赖
- 无外部组件库
- 无 JS 交互逻辑

## 当前实现状态

- 首页：已基于 Stitch 设计实现（Vue 3）
- 公共题库、题目详情、创建题目、我的题目、管理员全部题库、用户主页：第二轮实现
- 训练计划、讨论区、排行榜、OJ 账号等：后端开发后实现
- 登录/注册页：无独立 Stitch Screen，根据 Design System 自行设计
