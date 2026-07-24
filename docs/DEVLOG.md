# DEVLOG

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
- [ ] 用户注册和登录（待实现）
- [ ] JWT 认证（待实现）
- [ ] 业务接口（帖子、评论、训练计划等）（待实现）
- [ ] Codeforces 同步（待实现）
- [ ] Redis 排行榜优化（待实现）
- [ ] 测试（待实现）
