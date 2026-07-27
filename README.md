# ACMate

校内程序设计竞赛社的训练与题解交流平台。

## 技术栈

- Java 17 (JDK 21)
- Spring Boot 4.1.0
- MyBatis-Plus 3.5.17
- MySQL 8.0+
- Spring Security
- Maven

## 当前完成状态

- [x] 项目基础配置
- [x] 全量数据库设计（11 张表 SQL 已生成）
- [x] 数据库表创建
- [x] AppUser 实体和 Mapper
- [x] 数据库健康检查接口
- [x] 数据库健康检查验证
- [x] 最小 Spring Security 配置
- [x] 用户注册接口
- [x] Session 登录接口
- [x] 当前用户查询接口
- [x] 退出登录接口

## 本地启动

### 1. 创建数据库

```cmd
mysql -u root -p < src\main\resources\db\manual\00_create_database.sql
```

Flyway 在应用启动时自动执行所有迁移（V1-V11），无需手动导入 SQL 文件。

> 若数据库已有数据，Flyway 会检测已基线化版本，跳过已执行迁移。详见 `docs/DEVLOG.md`。

### 2. 配置环境变量

必须设置 `DB_PASSWORD`，可选覆盖 `DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USERNAME`。

```powershell
$env:DB_PASSWORD="your_password"
```

### 3. 编译启动

```powershell
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd -DskipTests spring-boot:run
```

## 健康检查

```bash
curl http://localhost:8080/api/health
```

响应示例：

```json
{"status":"UP","database":"UP","userCount":5}
```
