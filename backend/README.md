# 管理系统后端服务

基于 Spring Boot 3 + JWT 的管理系统后端API服务，支持管理员和普通用户两种角色。

## 技术栈

- **Spring Boot 3.2** - 应用框架
- **Spring Security** - 安全认证
- **JWT (jjwt)** - Token认证
- **Spring Data JPA** - 数据访问
- **H2 Database** - 内存数据库（开发环境）
- **MySQL** - 生产数据库
- **Swagger/OpenAPI** - API文档
- **Lombok** - 代码简化

## 项目结构

```
backend/
├── src/main/java/com/rpa/management/
│   ├── ManagementSystemApplication.java  # 启动类
│   ├── controller/                       # 控制器层
│   │   ├── AuthController.java          # 认证控制器
│   │   ├── AdminController.java         # 管理员控制器
│   │   └── UserController.java          # 用户控制器
│   ├── service/                          # 服务层
│   │   └── UserService.java
│   ├── entity/                           # 实体类
│   │   ├── User.java
│   │   └── UserRole.java
│   ├── repository/                       # 数据访问层
│   │   └── UserRepository.java
│   ├── dto/                              # 数据传输对象
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   └── ApiResponse.java
│   ├── config/                           # 配置类
│   │   ├── SecurityConfig.java          # 安全配置
│   │   ├── JwtAuthenticationFilter.java # JWT过滤器
│   │   └── GlobalExceptionHandler.java  # 异常处理
│   └── utils/                            # 工具类
│       └── JwtUtils.java                # JWT工具
├── src/main/resources/
│   └── application.yml                   # 配置文件
└── pom.xml                               # Maven配置
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- （可选）MySQL 8.0+

### 2. 运行项目

```bash
# 进入项目目录
cd backend

# 编译项目
mvn clean install

# 运行项目
mvn spring-boot:run
```

### 3. 访问地址

启动成功后，可以访问：

- **API文档**: http://localhost:8080/api/swagger-ui/index.html
- **H2控制台**: http://localhost:8080/api/h2-console
  - JDBC URL: `jdbc:h2:mem:management_db`
  - 用户名: `sa`
  - 密码: （空）

## API接口文档

### 1. 认证接口（无需登录）

#### 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应示例：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": "ADMIN",
    "roleDisplayName": "管理员",
    "email": "admin@example.com",
    "avatar": null
  },
  "timestamp": 1704067200000
}
```

### 2. 用户接口（需要登录）

#### 获取个人资料

```http
GET /api/user/profile
Authorization: Bearer {token}
```

#### 更新个人资料

```http
PUT /api/user/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "realName": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000"
}
```

### 3. 管理员接口（仅管理员）

#### 获取所有用户

```http
GET /api/admin/users
Authorization: Bearer {admin_token}
```

#### 创建用户

```http
POST /api/admin/users
Authorization: Bearer {admin_token}
Content-Type: application/json

{
  "username": "user01",
  "password": "user123",
  "realName": "用户01",
  "email": "user01@example.com"
}
```

## 用户角色说明

| 角色 | 角色代码 | 权限说明 |
|------|---------|---------|
| 管理员 | ADMIN | 拥有所有权限，可以管理用户、系统配置等 |
| 普通用户 | USER | 拥有基本权限，可以管理个人资料、查看任务等 |

## 默认账号

系统启动时会自动创建管理员账号：

- **用户名**: admin
- **密码**: admin123
- **角色**: 管理员

## 配置说明

### 切换到MySQL数据库

1. 修改 `application.yml`:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/management_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
  h2:
    console:
      enabled: false
```

2. 创建数据库:

```sql
CREATE DATABASE management_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 修改JWT密钥

在生产环境中，请务必修改JWT密钥：

```yaml
jwt:
  secret: your-production-secret-key-at-least-256-bits-long
  expiration: 86400000  # 24小时
```

## 使用Postman测试

### 1. 登录获取Token

1. 创建POST请求: `http://localhost:8080/api/auth/login`
2. Body选择raw → JSON，输入:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
3. 发送请求，复制返回的token

### 2. 使用Token访问受保护接口

1. 在请求头添加:
   ```
   Authorization: Bearer {你的token}
   ```
2. 访问用户接口: `GET http://localhost:8080/api/user/profile`

## 前端集成

前端需要在每个请求中携带Token：

```javascript
// axios配置示例
axios.defaults.baseURL = 'http://localhost:8080/api';
axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
```

修改前端登录逻辑：

```javascript
// frontend/src/views/Login.vue
const handleLogin = () => {
  loginFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true;
      // 调用真实的登录接口
      axios.post('http://localhost:8080/api/auth/login', {
        username: loginForm.username,
        password: loginForm.password
      }).then(response => {
        const { token, userId, username, role } = response.data.data;
        // 保存token到localStorage
        localStorage.setItem('token', token);
        localStorage.setItem('userInfo', JSON.stringify({ userId, username, role }));
        ElMessage.success('登录成功');
        router.push('/');
      }).catch(error => {
        ElMessage.error(error.response?.data?.message || '登录失败');
      }).finally(() => {
        loading.value = false;
      });
    }
  });
};
```

## 安全建议

1. **生产环境务必修改JWT密钥**
2. **使用HTTPS协议**
3. **定期更换密钥**
4. **设置合理的Token过期时间**
5. **对敏感接口进行权限控制**
6. **记录操作日志**

## 常见问题

### 1. 启动报错：端口被占用

修改 `application.yml` 中的端口号：

```yaml
server:
  port: 8081
```

### 2. 数据库连接失败

检查数据库配置是否正确，确保MySQL服务已启动。

### 3. Token验证失败

- 检查请求头是否正确：`Authorization: Bearer {token}`
- 确认Token未过期
- 验证JWT密钥配置一致

## 开发计划

- [ ] 添加刷新Token功能
- [ ] 添加记住我功能
- [ ] 添加验证码功能
- [ ] 添加操作日志记录
- [ ] 集成Redis缓存
- [ ] 添加单元测试

## 许可证

MIT License
