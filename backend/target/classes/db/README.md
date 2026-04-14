# 数据库说明文档

## 📁 文件列表

| 文件名 | 说明 |
|--------|------|
| `schema.sql` | 数据库表结构定义 |
| `data.sql` | 初始化数据 |
| `init-mysql.sql` | MySQL完整初始化脚本（建表+数据） |

---

## 🗄️ 数据表清单

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `sys_user` | 用户表 | id, username, password, role, status |
| `sys_role` | 角色表 | id, name, code, description |
| `sys_permission` | 权限表 | id, name, code, type, parent_id |
| `sys_role_permission` | 角色权限关联表 | role_id, permission_id |
| `sys_task` | 任务表 | id, task_id, name, type, status |
| `sys_robot` | 机器人表 | id, name, type, status |
| `sys_execution_log` | 执行日志表 | id, task_id, level, message |
| `sys_operation_log` | 操作日志表 | id, user_id, operation, ip |

---

## 🚀 快速使用

### 方式一：使用 MySQL 完整初始化脚本（推荐）

```bash
# 登录MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/init-mysql.sql

# 或直接执行
mysql -u root -p < init-mysql.sql
```

### 方式二：分步执行

```bash
# 1. 先执行建表脚本
mysql -u root -p < schema.sql

# 2. 再执行数据初始化脚本
mysql -u root -p management_system < data.sql
```

### 方式三：在 MySQL 客户端中执行

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS management_system 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE management_system;

-- 然后复制粘贴 schema.sql 和 data.sql 的内容执行
```

---

## 👤 默认账号

### 管理员账号
- **用户名**: `admin`
- **密码**: `admin123`
- **角色**: 管理员 (ADMIN)
- **权限**: 拥有所有权限

### 普通用户账号
- **用户名**: `user01`
- **密码**: `user123`
- **角色**: 普通用户 (USER)
- **权限**: 基本操作权限

### 其他测试账号
- **用户名**: `user02` / **密码**: `user123` (普通用户)
- **用户名**: `user03` / **密码**: `user123` (已禁用)

---

## 🔐 密码说明

所有密码使用 **BCrypt** 算法加密存储：

| 原始密码 | BCrypt 加密值 |
|---------|--------------|
| `admin123` | `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH` |
| `user123` | `$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2` |

> **注意**: BCrypt 每次加密的结果都不同，但验证时可以使用同一个密码验证成功。

---

## 📊 表关系

```
sys_user (用户表)
    └── role 字段关联 sys_role (逻辑关联)

sys_role (角色表)
    └── sys_role_permission (角色权限关联表)
        └── sys_permission (权限表)

sys_task (任务表)
    ├── user_id → sys_user.id (创建用户)
    └── robot_id → sys_robot.id (执行机器人)

sys_execution_log (执行日志表)
    └── task_id → sys_task.id
```

---

## 🔄 Spring Boot 自动建表

如果使用 Spring Boot 的 JPA 自动建表功能：

1. 确保 `application.yml` 中配置：
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 或 create-drop (仅开发环境)
```

2. 启动应用时会自动创建表结构

3. 初始数据需要通过以下方式插入：
   - 使用 `data.sql` (放在 `src/main/resources/` 下)
   - 或使用 `CommandLineRunner` 初始化

---

## 🛠️ 开发环境配置

### H2 内存数据库（默认）

```yaml
spring:
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:management_db;DB_CLOSE_DELAY=-1;MODE=MySQL
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
```

访问 H2 控制台：http://localhost:8080/api/h2-console
- JDBC URL: `jdbc:h2:mem:management_db`
- 用户名: `sa`
- 密码: 空

### MySQL 数据库（生产环境）

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/management_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
```

---

## ✅ 验证数据

执行以下SQL验证数据是否初始化成功：

```sql
-- 查看用户数据
SELECT id, username, real_name, role, status FROM sys_user;

-- 查看角色数据
SELECT * FROM sys_role;

-- 查看权限数据
SELECT id, name, code, type FROM sys_permission;

-- 查看机器人数据
SELECT id, name, type, status FROM sys_robot;
```

---

## 📝 注意事项

1. **生产环境安全**
   - 修改默认管理员密码
   - 删除测试账号
   - 使用强密码

2. **数据备份**
   - 定期备份数据库
   - 使用 mysqldump 工具

3. **字符集设置**
   - 确保数据库字符集为 `utf8mb4`
   - 排序规则为 `utf8mb4_unicode_ci`

---

## 🔧 常用操作

### 修改管理员密码

```sql
-- 新密码：newpassword123
UPDATE sys_user 
SET password = '$2a$10$YourNewBCryptPasswordHash' 
WHERE username = 'admin';
```

### 禁用用户

```sql
UPDATE sys_user SET status = 'inactive' WHERE username = 'user01';
```

### 查看用户权限

```sql
SELECT 
    u.username,
    u.role,
    p.name AS permission_name,
    p.code AS permission_code
FROM sys_user u
LEFT JOIN sys_role_permission rp ON rp.role_id = (
    SELECT id FROM sys_role WHERE code = u.role
)
LEFT JOIN sys_permission p ON p.id = rp.permission_id
WHERE u.username = 'admin';
```
