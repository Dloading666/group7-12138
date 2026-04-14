# 项目总览

## 项目结构

```
project11/projects/
├── frontend/          # Vue 3 前端项目
│   ├── src/
│   │   ├── views/    # 17个页面组件
│   │   ├── router/   # 路由配置
│   │   └── styles/   # 全局样式
│   └── package.json
│
└── backend/           # Spring Boot 后端项目
    ├── src/main/java/com/rpa/management/
    │   ├── controller/   # 控制器层
    │   ├── service/      # 服务层
    │   ├── entity/       # 实体类
    │   ├── config/       # 配置类
    │   └── utils/        # 工具类
    └── pom.xml
```

## 技术栈

### 前端
- Vue 3 + Composition API
- Element Plus UI组件库
- Vue Router 4
- Pinia状态管理
- Vite构建工具
- ECharts图表库

### 后端
- Spring Boot 3.2
- Spring Security + JWT认证
- Spring Data JPA
- H2 / MySQL数据库
- Swagger API文档

## 快速启动

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址: http://localhost:8080/api  
API文档: http://localhost:8080/api/swagger-ui/index.html

### 2. 启动前端

```bash
cd frontend
npm install  # 首次运行
npm run dev
```

前端地址: http://localhost:3000

## 默认账号

- 用户名: admin
- 密码: admin123
- 角色: 管理员

## API接口列表

### 认证接口
- POST `/api/auth/login` - 用户登录
- GET `/api/auth/userinfo` - 获取当前用户信息
- POST `/api/auth/logout` - 用户登出

### 用户接口（需要登录）
- GET `/api/user/profile` - 获取个人资料
- PUT `/api/user/profile` - 更新个人资料
- PUT `/api/user/password` - 修改密码
- GET `/api/user/tasks` - 获取我的任务

### 管理员接口（仅管理员）
- GET `/api/admin/users` - 获取所有用户
- POST `/api/admin/users` - 创建用户
- PUT `/api/admin/users/{id}/disable` - 禁用用户
- GET `/api/admin/statistics` - 获取系统统计

## 功能模块

### 前端（17个页面）
1. **系统管理**
   - 用户管理
   - 角色管理
   - 权限管理

2. **任务管理**
   - 任务列表
   - 创建任务
   - 任务历史

3. **流程定义与设计**
   - 流程列表
   - 流程设计器

4. **机器人管理**
   - 机器人列表
   - 机器人配置

5. **执行监控与记录**
   - 实时监控
   - 执行日志

6. **数据查询与统计**
   - 数据查询
   - 统计报表

7. **系统设置**
   - 基础设置
   - 通知设置

### 后端功能
- JWT Token认证
- 角色权限控制（ADMIN / USER）
- 密码加密存储（BCrypt）
- CORS跨域配置
- 统一异常处理
- API文档自动生成
- 数据库自动建表

## 前后端对接

前端需要修改登录接口调用真实后端API：

```javascript
// frontend/src/views/Login.vue
import axios from 'axios'

const handleLogin = () => {
  loginFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      axios.post('http://localhost:8080/api/auth/login', {
        username: loginForm.username,
        password: loginForm.password
      }).then(response => {
        const { token, userId, username, role } = response.data.data
        localStorage.setItem('token', token)
        localStorage.setItem('userInfo', JSON.stringify({ userId, username, role }))
        ElMessage.success('登录成功')
        router.push('/')
      }).catch(error => {
        ElMessage.error(error.response?.data?.message || '登录失败')
      }).finally(() => {
        loading.value = false
      })
    }
  })
}
```

## 测试接口

使用 `backend/test-api.http` 文件测试接口（VSCode需要安装REST Client插件）

## 生产部署

### 前端构建
```bash
cd frontend
npm run build
```

### 后端打包
```bash
cd backend
mvn clean package
java -jar target/management-system-1.0.0.jar
```

## 开发建议

1. 修改JWT密钥为生产环境密钥
2. 使用MySQL替代H2数据库
3. 配置HTTPS
4. 添加Redis缓存
5. 完善日志记录
6. 添加单元测试
