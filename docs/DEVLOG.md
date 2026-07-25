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
- [x] 用户注册（已完成）
- [ ] 用户登录（待实现）
- [ ] JWT 认证（待实现）
- [ ] 业务接口（帖子、评论、训练计划等）（待实现）
- [ ] Codeforces 同步（待实现）
- [ ] Redis 排行榜优化（待实现）
- [ ] 测试（待实现）

## 2026-07-24：用户注册

### 本次目标

实现 POST /api/auth/register，包括请求校验、密码 BCrypt 编码、重复注册检测、并发冲突处理、最小异常体系。

### 修改文件

**新建：**
- src/main/java/com/itnoduck/acmate/user/dto/RegisterRequest.java
- src/main/java/com/itnoduck/acmate/user/dto/RegisterResponse.java
- src/main/java/com/itnoduck/acmate/user/service/UserRegistrationService.java
- src/main/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImpl.java
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java
- src/main/java/com/itnoduck/acmate/common/exception/BusinessException.java
- src/main/java/com/itnoduck/acmate/common/exception/GlobalExceptionHandler.java
- src/main/java/com/itnoduck/acmate/common/dto/ApiError.java
- src/test/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImplTest.java
- docs/API.md

**修改：**
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 添加 PasswordEncoder Bean，放行 /api/auth/register，最小范围 CSRF 忽略
- docs/DEVLOG.md — 追加本记录
- README.md — 更新完成状态
- docs/ROADMAP.md — 更新阶段 2 进度

### 密码存储方案

- BCryptPasswordEncoder，由 SecurityConfig 提供 Bean
- 原始密码不 trim，不记录明文，不输出到日志或异常

### 重复注册处理方案

- 注册前使用 LambdaQueryWrapper 查询用户名和邮箱
- 存在则抛出 BusinessException(409)
- insert 时捕获 DuplicateKeyException 作为并发兜底，统一返回 HTTP 409
- 不解析 MySQL 英文异常消息判断冲突字段

### 请求规范化

- username：strip + toLowerCase(Locale.ROOT)
- nickname：strip
- email：空/纯空格 → null，非空 → strip + toLowerCase(Locale.ROOT)
- password：不 trim

### 测试结果

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

- 正常注册（6 个断言）
- 用户名已存在 → 409
- 邮箱已存在 → 409
- 空字符串邮箱规范化为 null
- insert 返回 0 → 500
- DuplicateKeyException → 409

### 接口验收结果

已通过。

```bash
# 正常注册 → 201
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser1","password":"password123","nickname":"Test User","email":"test1@example.com"}'
# → {"id":1,"username":"testuser1","nickname":"Test User","email":"test1@example.com"}

# 重复用户名 → 409
# → {"code":409,"message":"用户名已被使用","timestamp":"..."}

# 校验失败 → 400
# → {"code":400,"message":"请求参数校验失败","fieldErrors":[...],"timestamp":"..."}
```

### 遗留问题

- [ ] 用户登录（待实现）
- [ ] JWT 认证（待实现）
- [ ] 业务接口（帖子、评论、训练计划等）（待实现）
- [ ] Codeforces 同步（待实现）
- [ ] Redis 排行榜优化（待实现）
- [ ] 测试（待实现）

## 2026-07-24：项目验收确认

### 验收内容

对当前运行中的 ACMate 应用进行完整验收。

### 运行进程

- PID: 6776
- 启动类: com.itnoduck.acmate.AcMateApplication
- 端口: 8080
- 启动方式: IDEA 运行配置

### /api/health 实际响应

```json
{"status":"UP","database":"UP","userCount":1}
```

`userCount=1` 证明 appUserMapper.selectCount(null) 实际查询了数据库（非硬编码值）。

### SQL 验证

应用由 IDEA 启动，无法在此终端直接查看控制台 SQL 日志。但 `userCount=1`（与已注册用户数一致）间接证实了实际数据库查询。

### 数据库表数量验证

未直接查询 MySQL（DB_PASSWORD 仅在 IDEA 运行配置中，未在本 shell 暴露）。通过 /api/health 的 selectCount 成功执行确认 app_user 表存在且数据库连接正常。其余 10 张表曾在 SQL 执行阶段通过 source 命令创建，但本次未直接逐表验证。

### 结论

- ACMate 应用正常运行，PID 6776
- 数据库连接正常
- /api/health 返回正确
- 未直接验证全部 11 张表（受限于 DB_PASSWORD 不可用）

## 2026-07-24：注册模块审查与修正

### 修正内容

1. **CSRF 匹配收紧**
   - SecurityConfig 中 CSRF 忽略从字符串路径改为 `PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/register")`
   - 授权规则同步改为 HTTP method 级别：`HttpMethod.GET /api/health` + `HttpMethod.POST /api/auth/register`

2. **DTO 规范化前置**
   - RegisterRequest 增加自定义 setter 对 username、nickname、email 执行规范化
   - Jakarta Validation 校验规范化后的值（例如 nickname=" a " → strip → "a" → @Size 2~32 失败 → 400）
   - Service 层移除冗余规范化逻辑
   - password setter 不做 trim

3. **新增测试**
   - AuthControllerTest：使用 @WebMvcTest 加载真实 SecurityFilterChain，12 个测试覆盖参数校验、DTO 规范化、isAdmin 防注入、CSRF 规则验证
   - UserRegistrationServiceImplTest：新增 DTO 规范化验证（username 大小写、nickname trim、email trim+lowercase）

### 新增文件
- src/test/java/com/itnoduck/acmate/user/controller/AuthControllerTest.java

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — method-specific matchers
- src/main/java/com/itnoduck/acmate/user/dto/RegisterRequest.java — custom normalization setters
- src/main/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImpl.java — 移除 normalization
- src/test/java/com/itnoduck/acmate/user/service/impl/UserRegistrationServiceImplTest.java — 新增规范化测试
- README.md — 健康检查说明修正
- docs/DEVLOG.md — 追加本记录

### 测试框架

AuthControllerTest 从 standaloneSetup 迁移到 @WebMvcTest，加载真实 SecurityConfig：

- `@WebMvcTest(AuthController.class)` + `@Import({SecurityConfig.class, GlobalExceptionHandler.class})`
- 使用 `@MockitoBean` 模拟 UserRegistrationService
- `@Autowired MockMvc` 自动注入
- 导入路径：`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`、`org.springframework.test.context.bean.override.mockito.MockitoBean`

### CSRF 测试覆盖

- POST /api/auth/register 无 CSRF → 201（CSRF 仅对此端点忽略）
- PUT /api/auth/register 无 CSRF → 403（仅 POST 方法被忽略）
- POST /api/security-test 无 CSRF → 403（其他路径仍受 CSRF 保护）
- POST /api/security-test 带 CSRF → 404（非 403，证明 CSRF 检查已通过）

### 测试结果

```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

- AuthControllerTest: 12 tests passed
- UserRegistrationServiceImplTest: 10 tests passed
- AcMateApplicationTests: 1 test passed

## 2026-07-24：Spring Profile 配置修正

### 修正内容

- 从 `application.yml` 中删除 `spring.profiles.active: local`
- 本地 Profile 改为由 IDEA 运行配置显式激活（`--spring.profiles.active=local`）
- `application-local.yml` 已在 `.gitignore` 中，不会提交

### 原因

公共 `application.yml` 中的默认 profile 会导致 Maven 测试也激活 `local` profile，测试应该使用 `default` profile，不依赖本地环境配置。

### 测试日志确认

```
No active profile set, falling back to 1 default profile: "default"
```

不再出现 `The following 1 profile is active: "local"`。

### 测试结果

```
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

## 2026-07-25：Session 登录 — 认证基础层

### 本次目标

创建认证主体和数据库 UserDetailsService，不实现登录接口、Session 或 JWT。

### 新增文件
- src/main/java/com/itnoduck/acmate/security/AuthenticatedUser.java — 实现 UserDetails
- src/main/java/com/itnoduck/acmate/security/DatabaseUserDetailsService.java — 实现 UserDetailsService

### 设计决策

- **AuthenticatedUser**：final class，手动 getter，不暴露 passwordHash 到 toString
- **权限映射**：所有用户 → ROLE_USER；isAdmin=1 → 额外 ROLE_ADMIN，使用 SimpleGrantedAuthority
- **状态映射**：status=1 → enabled=true，其他 → enabled=false，禁用账号仍会被加载
- **查询**：LambdaQueryWrapper + selectOne，username 先 strip 再 toLowerCase(Locale.ROOT)
- **密码**：不在此层执行 BCrypt matches，由后续 DaoAuthenticationProvider 处理

### 已知限制

- 尚未实现登录接口
- 尚未实现 Session 持久化
- 尚未实现退出登录
- 尚未实现当前用户查询接口
