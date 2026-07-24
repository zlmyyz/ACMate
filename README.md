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

## 本地启动

### 1. 创建数据库和表

以下命令适用于 CMD（不适用于 PowerShell 的 `<` 重定向）：

```cmd
mysql -u root -p < src\main\resources\db\manual\00_create_database.sql
mysql -u root -p acmate < src\main\resources\db\migration\V1__create_app_user.sql
mysql -u root -p acmate < src\main\resources\db\migration\V2__create_problem_training_tables.sql
mysql -u root -p acmate < src\main\resources\db\migration\V3__create_discussion_tables.sql
mysql -u root -p acmate < src\main\resources\db\migration\V4__create_oj_sync_tables.sql
```

PowerShell 替代方式：

```powershell
Get-Content src\main\resources\db\manual\00_create_database.sql | mysql -u root -p
Get-Content src\main\resources\db\migration\V1__create_app_user.sql | mysql -u root -p acmate
# ... 依次执行其余迁移文件
```

或使用 MySQL 客户端 source 命令：

```
mysql -u root -p
source src/main/resources/db/manual/00_create_database.sql
use acmate;
source src/main/resources/db/migration/V1__create_app_user.sql
source src/main/resources/db/migration/V2__create_problem_training_tables.sql
source src/main/resources/db/migration/V3__create_discussion_tables.sql
source src/main/resources/db/migration/V4__create_oj_sync_tables.sql
```

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

实际返回：

```json
{"status":"UP","database":"UP","userCount":0}
```
