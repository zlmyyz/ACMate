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

## 2026-07-25：Session 登录

### 本次目标

实现 POST /api/auth/login、GET /api/users/me、Session 持久化，不实现退出登录。

### 新增文件
- src/main/java/com/itnoduck/acmate/user/dto/LoginRequest.java — 登录请求（无 @Data，手动 toString 排除 password）
- src/main/java/com/itnoduck/acmate/user/dto/LoginResponse.java — 登录响应 record
- src/main/java/com/itnoduck/acmate/user/dto/CurrentUserResponse.java — 当前用户响应 record
- src/main/java/com/itnoduck/acmate/user/service/UserAuthenticationService.java — 登录服务接口
- src/main/java/com/itnoduck/acmate/user/service/impl/UserAuthenticationServiceImpl.java — 登录认证实现
- src/main/java/com/itnoduck/acmate/user/controller/UserController.java — GET /api/users/me
- src/main/java/com/itnoduck/acmate/security/RestAuthenticationEntryPoint.java — 401 JSON 响应
- src/test/java/com/itnoduck/acmate/user/controller/SessionLoginTest.java — 12 个登录 Session 集成测试

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 AuthenticationManager、SecurityContextRepository、SessionAuthenticationStrategy、RestAuthenticationEntryPoint Bean；放行 /api/auth/login；保护 /api/users/me
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java — 增加 POST /api/auth/login
- src/main/java/com/itnoduck/acmate/common/exception/GlobalExceptionHandler.java — 增加 BadCredentialsException/UsernameNotFoundException → 401、DisabledException → 403
- src/main/java/com/itnoduck/acmate/common/dto/ApiError.java — Jackson 3.x import 修正
- src/main/resources/application.yml — 增加 Session 超时和 Cookie 配置
- src/test/java/com/itnoduck/acmate/user/controller/AuthControllerTest.java — 增加 @MockitoBean 适配新依赖
- docs/API.md — 增加登录和 /me 接口文档
- docs/DEVLOG.md — 追加本记录

### AuthenticationManager 配置

```java
DaoAuthenticationProvider provider = new DaoAuthenticationProvider(databaseUserDetailsService);
provider.setPasswordEncoder(passwordEncoder);
new ProviderManager(provider);
```

### 登录认证链路

1. AuthController 接收 LoginRequest → UserAuthenticationService.login()
2. UsernamePasswordAuthenticationToken(username, rawPassword)
3. AuthenticationManager.authenticate() → DaoAuthenticationProvider
4. DatabaseUserDetailsService.loadUserByUsername() → 查询用户
5. BCryptPasswordEncoder.matches() → 验证密码
6. 成功 → SecurityContext → SecurityContextHolder + HttpSession
7. ChangeSessionIdAuthenticationStrategy 更换 Session ID

### SecurityContext 写入 Session

通过 SecurityContextRepository.saveContext(context, request, response) 将 SecurityContext 持久化到 HttpSession，后续请求可通过 JSESSIONID Cookie 恢复登录状态。

### Session Fixation 防护

ChangeSessionIdAuthenticationStrategy 在认证成功时自动更换 Session ID，防止 Session Fixation 攻击。

### 异常处理

| 异常 | HTTP | 消息 |
|------|------|------|
| BadCredentialsException | 401 | 用户名或密码错误 |
| UsernameNotFoundException | 401 | 用户名或密码错误 |
| DisabledException | 403 | 账号已被禁用 |

### 测试结果

```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
```

- SessionLoginTest: 12 tests（正确凭证、密码错误、用户不存在、禁用用户、CSRF 忽略、Session 存储、跨请求 /me、响应字段验证、未登录 401、Principal 类型验证、管理员标识）
- AuthControllerTest: 12 tests（注册相关测试仍通过）
- UserRegistrationServiceImplTest: 10 tests
- AcMateApplicationTests: 1 test

### 已知限制

- 尚未实现退出登录

## 2026-07-25：Session 退出登录

### 本次目标

实现 POST /api/auth/logout，包括 SecurityContext 清除、Session 失效、JSON 403 响应、测试和文档。

### 新建文件
- src/main/java/com/itnoduck/acmate/security/RestAccessDeniedHandler.java — JSON 403 响应
- src/test/java/com/itnoduck/acmate/user/controller/LogoutTest.java — 9 个退出登录测试

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 RestAccessDeniedHandler Bean，exceptionHandling 增加 accessDeniedHandler
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java — 增加 POST /api/auth/logout
- docs/API.md — 增加 logout 接口文档
- docs/DEVLOG.md — 追加本记录

### logout 实现方式

Controller 中注入 Authentication（Spring MVC 参数自动绑定），调用 SecurityContextLogoutHandler.logout(request, response, authentication)：

1. SecurityContextLogoutHandler 负责：清除 SecurityContextHolder + 调用 SecurityContextRepository.saveContext() 将空 Context 写回 Session + 使 HttpSession 失效
2. SecurityContextRepository（HttpSessionSecurityContextRepository）负责：从 Session 读取/写入 SecurityContext，在此流程中完成 Session 属性清理
3. SecurityContextHolder.clearContext() 仅清理当前线程，不足以使 Session 失效

返回 204 No Content，响应体为空。

### CSRF 防护

- POST /api/auth/logout 不在 CSRF 忽略列表
- 必须携带有效 CSRF Token
- 缺少 CSRF → 403 JSON（AccessDeniedHandler）
- 未认证 → 401 JSON（AuthenticationEntryPoint）

### 401 vs 403 区分

- 401：未认证（未登录或 Session 已失效）→ AuthenticationEntryPoint
- 403：已认证但缺少/无效 CSRF → AccessDeniedHandler（新增 RestAccessDeniedHandler）

### 测试结果

```
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
```

- LogoutTest: 9 tests（无 CSRF → 403、有 CSRF → 204、Session 失效、logout 后 /me → 401、SecurityContext 清除、未认证 → 401、响应体为空、注册和登录 CSRF 豁免不退化、其他 POST 无 CSRF → 403）
- SessionLoginTest: 12 tests
- AuthControllerTest: 12 tests
- UserRegistrationServiceImplTest: 10 tests
- AcMateApplicationTests: 1 test

### 已知限制

- Session 认证阶段（login + me + logout）已完整实现

## 2026-07-25：CSRF Token 获取接口

### 本次目标

为真实客户端（非测试 built-in helper）提供 CSRF Token 获取端点 GET /api/auth/csrf，完成跨请求真实 Token 流程测试。

### 新建文件
- src/main/java/com/itnoduck/acmate/user/dto/CsrfTokenResponse.java — record(token, headerName, parameterName)

### 修改文件
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — GET /api/auth/csrf → permitAll
- src/main/java/com/itnoduck/acmate/user/controller/AuthController.java — 增加 GET /api/auth/csrf
- src/test/java/com/itnoduck/acmate/user/controller/LogoutTest.java — 增加 7 个 CSRF Token 获取和真实流程测试
- docs/API.md — 增加 GET /api/auth/csrf 文档和客户端使用流程
- docs/DEVLOG.md — 追加本记录

### CSRF Token 获取方式

客户端通过 GET /api/auth/csrf 获取当前 Session 的 Token。CsrfFilter 在每次请求时通过 CsrfTokenRepository 生成/加载 Token 并存入 request attribute。

Controller 从 request.getAttribute(CsrfToken.class.getName()) 提取 Token，返回 token、headerName、parameterName。

### 真实客户端流程

1. POST /api/auth/login → 获取 JSESSIONID Cookie
2. GET /api/auth/csrf（同一 Session）→ 获取 token + headerName
3. POST /api/auth/logout（同一 Session）→ 设置请求头 `<headerName>: <token>` → 204

### SecurityContextLogoutHandler 与 SecurityContextRepository 职责区分

| 组件 | 职责 |
|------|------|
| SecurityContextLogoutHandler | 调用 SecurityContextHolder.clearContext() + 触发 SecurityContextRepository.saveContext(empty) + 使 Session 失效 |
| SecurityContextRepository | 从 Session 读取/写入 SecurityContext 属性（SPRING_SECURITY_CONTEXT），不负责 Session 生命周期 |
| SecurityContextHolder | 线程绑定的 Context，clearContext() 只影响当前线程 |

### 测试结果

```
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0
```

- LogoutTest: 15 tests（新增：CSRF 接口 200、Token 非空、响应不含 sessionId、真实 Token 登录后 logout → 204、错误 Token → 403、真实流程 login → csrf → logout → /me → 401）
- SessionLoginTest: 12 tests
- AuthControllerTest: 12 tests
- UserRegistrationServiceImplTest: 10 tests
- AcMateApplicationTests: 1 test

### 已知限制

- Session 认证阶段（login + me + logout + csrf）已完整实现

## 2026-07-25：Session 认证真实运行验收

### 验收时间

2026-07-25，本地运行环境，Maven + local profile。

### 验收流程

1. POST /api/auth/register → 201（新建验收用户）
2. POST /api/auth/login → 200（无 password 或 passwordHash 字段）
3. GET /api/users/me → 200（用户信息与登录一致）
4. GET /api/auth/csrf → 200（token 非空，含 headerName 和 parameterName）
5. POST /api/auth/logout + CSRF header → 204（响应体为空）
6. GET /api/users/me（同一 Session）→ 401（Session 已失效）
7. POST /api/auth/login（错误密码）→ 401
8. GET /api/users/me（未登录）→ 401（JSON 格式，非 HTML 或 302）
9. POST /api/auth/logout（无 CSRF）→ 403

### 自动化测试 vs 真实运行

| 维度 | 自动化测试 | 真实运行验收 |
|------|-----------|-------------|
| 环境 | @WebMvcTest + mock Mapper | Maven spring-boot:run + 真实 MySQL |
| Session | MockHttpSession | 真实 Tomcat HttpSession + JSESSIONID Cookie |
| CSRF | with(csrf()) post-processor | 真实 GET /api/auth/csrf → 提取 token → 设置请求头 |
| 认证 | with(user(...)) 或 mock login | 真实 BCrypt + AuthenticationManager + DB 查询 |

两者在所有接口行为上完全一致。

### 已知限制

- Session 认证模块已全面验收通过

## 2026-07-25：题目查询模块

### 本次目标

实现题目实体、Mapper、分页配置、题目详情查询、题目分页列表，包含 DTO、Service、Controller、测试和文档。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/entity/Problem.java — 题目实体
- src/main/java/com/itnoduck/acmate/problem/mapper/ProblemMapper.java — Mapper 接口
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemQueryRequest.java — 列表查询参数
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemSummaryResponse.java — 列表项响应
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemDetailResponse.java — 详情响应
- src/main/java/com/itnoduck/acmate/common/dto/PageResponse.java — 通用分页响应
- src/main/java/com/itnoduck/acmate/problem/service/ProblemQueryService.java — 查询服务接口
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImpl.java — 查询服务实现
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 查询接口
- src/main/java/com/itnoduck/acmate/config/MybatisPlusConfig.java — 分页插件配置
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java — Service 测试（9 tests）
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — Controller 测试（7 tests）

### 修改文件
- pom.xml — 增加 mybatis-plus-jsqlparser 3.5.17（分页插件必需）
- docs/API.md — 增加 GET /api/problems 和 GET /api/problems/{id}
- docs/DEVLOG.md — 追加本记录

### 实体与 Mapper

- Problem 实体映射 problem 表，@TableName("problem")，id 使用 @TableId(type = IdType.AUTO)
- tags 字段当前为 VARCHAR(255) 逗号分隔字符串，暂不拆分为关联表
- ProblemMapper extends BaseMapper<Problem>，无 XML Mapper，无手写 SQL

### 分页插件配置

- MybatisPlusInterceptor + PaginationInnerInterceptor(DbType.MYSQL)
- maxLimit=100 为数据库层兜底，接口 DTO 层仍有 @Min/@Max 独立校验
- 需单独引入 mybatis-plus-jsqlparser，不通过 mybatis-plus-spring-boot4-starter 传递

### 动态查询条件

- 固定过滤 status=1，保证普通用户只看到正常题目
- platform 精确匹配，difficulty 精确匹配
- keyword 使用括号组合匹配 title OR externalProblemKey，防止 OR 绕过 status=1 过滤
- 排序：createTime DESC, id DESC

### status 过滤原因

Service 注释明确说明：status=0 的禁用题目不应被普通用户看到，用户不应感知题目"曾经存在但已禁用"的事实。所有查询入口统一过滤，不做按角色区分。

### 测试结果

```
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemQueryServiceImplTest: 9 tests（正常查询、不存在的 id→404、零/负 id→404、禁用题目不可见、status 过滤、platform 过滤、difficulty 过滤、keyword 括号组合、Entity→DTO 转换）
- ProblemControllerTest: 7 tests（未认证→401、已认证列表→200、已认证详情→200、不存在→404、page=0→400、size=101→400、非法 platform→400）
- 原 50 tests 全部通过

### 已知限制

- 尚未实现创建、修改、删除题目
- 尚未实现管理员接口
- 尚未实现训练计划、用户做题状态

## 2026-07-26：管理员创建题目

### 本次目标

实现 POST /api/problems，仅限 ROLE_ADMIN 创建，服务端指定 creatorUserId 和 status，前置查重 + DuplicateKeyException 并发兜底。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/dto/CreateProblemRequest.java — 创建请求 DTO，无 creatorUserId/status 字段，setter 内完成规范化
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 创建服务接口
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 创建服务实现
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — Service 测试（12 tests）

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 增加 POST /api/problems 端点
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 hasRole("ADMIN") 保护 POST /api/problems
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 增加 10 个 POST 测试
- docs/API.md — 增加 POST /api/problems 文档
- docs/DEVLOG.md — 追加本记录

### 信任边界

- creatorUserId 来自 @AuthenticationPrincipal AuthenticatedUser，不由请求体指定
- 请求体中的 creatorUserId 和 status 字段（无论值为何）均被忽略
- status 由服务端固定为 1

### 重复检测策略

- 前置查重：externalProblemKey 非空时 selectCount by platform + externalProblemKey，命中则 409
- 数据库兜底：catch DuplicateKeyException → 409（唯一索引 uk_platform_problem）
- 前置查重只用于友好错误提示，并发安全依赖唯一索引

### 权限控制

- SecurityFilterChain 中 .requestMatchers(HttpMethod.POST, "/api/problems").hasRole("ADMIN")
- 不使用 @PreAuthorize / @EnableMethodSecurity
- POST /api/problems 不在 CSRF 忽略列表，必须携带有效 CSRF Token

### DTO 规范化

- platform → uppercase（setter）
- externalProblemKey / title → strip，blank → null（setter）
- sourceUrl / difficulty → strip，blank → null（setter）
- tags → split→strip→dedup（LinkedHashSet 保留首次出现顺序）→逗号重连，empty → null（setter）
- contentMd → 不 trim（Markdown 白空格可能有意为之）
- @Getter 只读，手动 toString() 排除 contentMd

### 测试结果

```
Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemCommandServiceImplTest: 12 tests（正常创建、creatorUserId 来自参数、status 固定为 1、非 CUSTOM 必填 externalKey→400、CUSTOM 允许 null externalKey、前置查重→409、DuplicateKeyException→409、insert 返回 0→500、id 未回填→500、tags 规范化、blank→null、Entity→DTO 转换）
- ProblemControllerTest: 17 tests（原有 7 个查询 + 新增 10 个：未认证 POST+csrf→401、未认证 POST 无 csrf→403、普通用户+csrf→403、admin 无 csrf→403、admin+csrf→201、响应无 status、creatorUserId 传入 service=1L、请求体 creatorUserId/status 被忽略→仍传 1L、校验失败→400、GET 仍对普通用户可用）
- 原有 59 tests 全部通过

### 真实接口验收（2026-07-26）

使用运行中 ACMate + 本地 MySQL，真实 Session + CSRF Token 流程验收。

| # | 场景 | 预期 | 实际 | 结果 |
|---|------|------|------|------|
| 1 | Admin + CSRF → POST CUSTOM | 201 | 201 | 通过 |
| 2 | Normal user + CSRF → POST | 403 | 403 | 通过 |
| 3 | Admin 无 CSRF → POST | 403 | 403 | 通过 |
| 4 | CODEFORCES 无 externalProblemKey | 400 | 400 | 通过 |
| 5 | CUSTOM 无 externalProblemKey | 201 | 201 | 通过 |
| 6a | CODEFORCES + externalProblemKey | 201 | 201 | 通过 |
| 6b | 相同 platform+key 再次创建 | 409 | 409 | 通过 |
| 7 | tags=" array, hash-map, array,  " | 201, 存为 "array,hash-map" | 201, "array,hash-map" | 通过 |

**数据库核对：**
- 所有创建题目 status = 1，creator_user_id = 1（管理员 id）
- 创建响应和查询响应均不含 status 字段
- 普通用户 GET /api/problems 可正常分页查询
- 验收数据已全部删除，数据库恢复验收前状态

## 2026-07-26：权限模型修正 — 允许所有登录用户创建题目

### 本次目标

将 POST /api/problems 的权限从 ROLE_ADMIN 修正为所有已登录用户（authenticated），使普通用户也能创建题目。

### 修改文件

- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — hasRole("ADMIN") → authenticated()
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 补全类级和方法级中文 Javadoc
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 注释修正为"当前登录用户创建题目"
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 普通用户 403 → 201，新增 CSRF+普通用户测试
- docs/API.md — 权限描述修正为"所有已登录用户"
- docs/DEVLOG.md — 追加本记录

### 权限模型

- 题目查询和创建是所有登录用户的基础权限
- 管理员是普通用户权限的超集
- 资源所有权由 creatorUserId 记录
- 后续资源级管理操作将基于"创建者或管理员"判断
- CSRF 规则没有放宽
- 没有修改数据库结构
- 没有实现新的接口

### 测试结果

```
Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemControllerTest: 18 tests（新增 shouldReturn201WhenNormalUserPostsWithCsrf、shouldReturn403WhenNormalUserPostsWithoutCsrf；原 403 测试改为 201）
- ProblemCommandServiceImplTest: 12 tests 全部通过（Service 逻辑未改动）
- 原有测试全部通过

### 已知限制

- 尚未实现删除、停用题目

## 2026-07-26：创建者和管理员修改题目

### 本次目标

实现 PUT /api/problems/{id}，允许题目创建者或管理员完整修改题目，包含所有权校验、题目标识查重（排除自身）、事务保护、Controller 级中文 Javadoc。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/support/ProblemFieldNormalizer.java — 输入字段规范化工具，供 CreateProblemRequest 和 UpdateProblemRequest 共享
- src/main/java/com/itnoduck/acmate/problem/dto/UpdateProblemRequest.java — 修改请求 DTO，字段和规则与 CreateProblemRequest 一致，无 creatorUserId/status

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/dto/CreateProblemRequest.java — tags 规范化改用 ProblemFieldNormalizer
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 增加 updateProblem 方法签名
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 增加 updateProblem 实现，包含所有权校验、查重排除自身、LambdaUpdateWrapper null 支持、事务、回读
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 增加 PUT /api/problems/{id} 端点，补全全部 4 个端点中文 Javadoc
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — PUT /api/problems/{id} 使用 authenticated()
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — 增加 28 个 update 测试
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 增加 18 个 PUT 测试
- docs/API.md — 增加 PUT /api/problems/{id} 文档
- docs/DEVLOG.md — 追加本记录

### 设计决策

- **LambdaUpdateWrapper.set()** 而非 updateById：MyBatis-Plus 全局字段策略 NOT_NULL 会忽略 null 字段，.set() 显式控制
- **UPDATE WHERE status=1**：防止并发禁用后的修改
- **查重排除自身**：`.ne(Problem::getId, problemId)` 允许保持原值
- **更新后回读**：确保响应含 ON UPDATE CURRENT_TIMESTAMP 的最新 updateTime
- **@Transactional**：查询 → 校验 → 更新 → 回读的读写边界一致
- **权限在 Service 判断**：查询资源后判断 `operatorAdmin || Objects.equals(creatorUserId, operatorUserId)`
- **ProblemFieldNormalizer**：CreateProblemRequest 和 UpdateProblemRequest 共享同一套规范化逻辑

### 权限模型

- SecurityFilterChain：PUT /api/problems/{id} → authenticated()
- Service 层：ensureCanManageProblem() — 管理员或创建者，否则 403
- 区分 400（参数无效）、401（未登录）、403（CSRF 或权限）、404（不存在/禁用）、409（标识冲突）

### 测试结果

```
Tests run: 133, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemCommandServiceImplTest: 40 tests（12 创建 + 28 更新）
- ProblemControllerTest: 34 tests（6 查询 + 10 创建 + 18 PUT）
- 原有 59 tests 全部通过

### 已知限制

- 尚未实现删除、停用、个人题目管理

## 2026-07-26：个人题目管理与停用题目可见性调整

### 本次目标

新增 GET /api/problems/mine，调整停止题目的查看和编辑权限，使创建者和管理员可以查看和编辑停用题目。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/dto/MineProblemStatusFilter.java — ALL/ACTIVE/INACTIVE 状态筛选枚举
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemStatusView.java — ACTIVE/INACTIVE 状态视图，不暴露数据库数字
- src/main/java/com/itnoduck/acmate/problem/dto/MyProblemSummaryResponse.java — 含 status 和 updateTime 的列表项

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/service/ProblemQueryService.java — getProblem 增加 viewerUserId/viewerAdmin；新增 listMyProblems
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImpl.java — getProblem 先查后判；实现 listMyProblems
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 不再限制 status=1；非创建者非管理员对停用题目返回 404
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — GET /mine；getProblem 传递查看者身份；更新全部 Javadoc
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java — 9→18 tests
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — 40→43 tests
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 34→44 tests
- docs/API.md — 新增 GET /api/problems/mine；更新详情和修改接口
- docs/DEVLOG.md — 追加本记录

### 设计决策

- **MineProblemStatusFilter / ProblemStatusView**：接口层不暴露数据库数字状态
- **creatorUserId 来自认证主体**：不接受客户端传入，避免越权
- **公共列表不返回 status**：避免向其他用户暴露题目管理状态
- **停用题目对无关用户返回 404**：不暴露存在性
- **编辑停用题目不自动恢复**：仅修改内容字段，status 保持 0
- **getProblem 先查后判**：改为查询后根据查看者身份判断可见性

### 测试结果

```
Tests run: 155, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemQueryServiceImplTest: 18 tests
- ProblemCommandServiceImplTest: 43 tests
- ProblemControllerTest: 44 tests
- 原有 62 tests 全部通过

### 已知限制

- 尚未实现停用和恢复题目

## 2026-07-26：题目停用与恢复

### 本次目标

新增 POST /api/problems/{id}/deactivate 和 POST /api/problems/{id}/restore，允许创建者和管理员停用和恢复题目。停用是逻辑操作，不物理删除。

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/service/ProblemCommandService.java — 新增 deactivateProblem、restoreProblem 方法
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImpl.java — 停用和恢复实现
- src/main/java/com/itnoduck/acmate/problem/controller/ProblemController.java — 新增两个 POST 端点及 Javadoc
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 新增两条 authenticated() 规则
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemCommandServiceImplTest.java — 64 tests（+21）
- src/test/java/com/itnoduck/acmate/problem/controller/ProblemControllerTest.java — 59 tests（+15）
- docs/API.md / docs/DEVLOG.md — 追加记录

### 设计决策

- **幂等**：重复停用/恢复直接返回 204，不报错
- **WHERE 带原状态**：`eq(status, 1)` 或 `eq(status, 0)`，防并发
- **停用后仍占用标识**：platform+externalProblemKey 唯一性不变
- **停用题目对其他用户返回 404**：私有管理操作，不暴露存在性
- **先查后判权限**：无法在 URL 层完成
- **operatorUserId 来自 @AuthenticationPrincipal**：不由请求指定

### 测试结果

```
Tests run: 191, Failures: 0, Errors: 0, Skipped: 0
```

- ProblemCommandServiceImplTest: 64 tests
- ProblemControllerTest: 59 tests
- ProblemQueryServiceImplTest: 18 tests
- 原有 50 tests 全部通过

### 已知限制

- 尚未实现物理删除

## 2026-07-26：按创建者查看公开题目与管理员全站题库查询

### 本次目标

扩展 GET /api/problems 支持可选 creatorUserId 参数，新增 GET /api/admin/problems 管理员全站题库查询（含创建者信息批量加载）。

### 新建文件
- src/main/java/com/itnoduck/acmate/problem/dto/AdminProblemSummaryResponse.java — 含创建者信息的列表项 record
- src/main/java/com/itnoduck/acmate/problem/service/AdminProblemQueryService.java — 管理员查询服务接口
- src/main/java/com/itnoduck/acmate/problem/service/impl/AdminProblemQueryServiceImpl.java — 管理员查询实现，含批量用户加载
- src/main/java/com/itnoduck/acmate/problem/controller/AdminProblemController.java — 管理员题目查询控制器
- src/test/java/com/itnoduck/acmate/problem/service/impl/AdminProblemQueryServiceImplTest.java — 9 tests
- src/test/java/com/itnoduck/acmate/problem/controller/AdminProblemControllerTest.java — 9 tests

### 修改文件
- src/main/java/com/itnoduck/acmate/problem/dto/ProblemQueryRequest.java — 增加 creatorUserId 字段
- src/main/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImpl.java — listProblems 支持 creatorUserId 过滤
- src/main/java/com/itnoduck/acmate/config/SecurityConfig.java — 增加 /api/admin/** → hasRole("ADMIN")
- src/test/java/com/itnoduck/acmate/problem/service/impl/ProblemQueryServiceImplTest.java — 23 tests（+5）
- docs/API.md — 增加 GET /api/admin/problems，更新 GET /api/problems
- docs/DEVLOG.md — 追加本记录

### 设计决策

- **creatorUserId 过滤**：公共列表和全家列表均支持，值 ≤ 0 时返回 400
- **AdminProblemSummaryResponse**：扩展 ProblemSummaryResponse，增加 creatorUsername、creatorNickname、status、updateTime
- **批量用户加载**：收集分页结果中的唯一 creatorUserId，selectBatchIds 一次查询，build Map，避免 N+1
- **缺失创建者数据不抛异常**：username/nickname 返回 null，保留题目记录
- **ALL 不附加 status 条件**：管理员可查看全站正常和停用全部题目
- **ACTIVE/INACTIVE**：与 /mine 共享 MineProblemStatusFilter 枚举

### 测试结果

```
Tests run: 214, Failures: 0, Errors: 0, Skipped: 0
```

- AdminProblemControllerTest: 9 tests
- AdminProblemQueryServiceImplTest: 9 tests
- ProblemQueryServiceImplTest: 23 tests（+5）
- ProblemControllerTest: 59 tests
- ProblemCommandServiceImplTest: 64 tests
- 原有 50 tests 全部通过

### 已知限制

- 尚未实现物理删除

## 2026-07-26：Vue 前端初始化与认证

### 本次目标

初始化 Vue 3 前端工程，实现登录、注册、首页、Session 认证、CSRF 处理、403/404 页面和响应式基础。

### 新建文件

- frontend/ — Vue 3 + TypeScript + Vite 工程
- frontend/src/styles/tokens.css — Design Tokens（CSS Variables）
- frontend/src/styles/main.css — 全局样式与字体
- frontend/src/constants/labels.ts — 中文界面文案常量
- frontend/src/api/client.ts — Axios 实例（withCredentials）
- frontend/src/api/auth.ts — 认证 API（login/register/me/csrf/logout）
- frontend/src/stores/auth.ts — Pinia 认证 Store
- frontend/src/router/index.ts — Vue Router（路由守卫）
- frontend/src/components/layout/AppHeader.vue — 顶部导航
- frontend/src/components/layout/PageContainer.vue — 页面容器
- frontend/src/components/common/LoadingState.vue — 加载状态
- frontend/src/components/common/EmptyState.vue — 空状态
- frontend/src/views/LoginView.vue — 登录页
- frontend/src/views/RegisterView.vue — 注册页
- frontend/src/views/HomeView.vue — 首页
- frontend/src/views/ForbiddenView.vue — 403 页
- frontend/src/views/NotFoundView.vue — 404 页
- frontend/src/__tests__/auth.test.ts — 11 个认证测试
- frontend/design/ — Stitch 设计资源（16 页截图 + HTML + theme JSON）
- frontend/design/README.md — 设计资源说明
- docs/PRD.md — 产品需求文档 v1.4

### 设计决策

- 样式方案：CSS Variables + scoped CSS + Element Plus，未使用 Tailwind
- 字体：Google Fonts CDN 引用 Hanken Grotesk + Inter + JetBrains Mono，CSS 回退到系统字体
- 认证：Session Cookie（withCredentials），不读取/保存 JSESSIONID
- CSRF：登出前 GET /api/auth/csrf 获取动态 Token
- 路由守卫：requiresAuth 跳转登录，guestOnly 跳转首页
- 初始化：init() 完成后挂载 app，避免 flicker
- 中文化：固定文案使用简体中文，枚举/变量保持英文
- 未实现模块：首页"即将推出"区域展示，不放导航

### 测试结果

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
```

### 构建结果

BUILD SUCCESS（type-check + lint + build + test 全部通过）

### 已知限制

- 尚未实现题库、题目详情、创建/编辑题目、我的题目、管理员页面
- 尚未实现训练计划、讨论区、排行榜、OJ 账号
- 尚未进行后端联调
