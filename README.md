# RPA 管理系统

一个面向 RPA 场景的后台管理项目，采用前后端分离架构，核心能力围绕“权限树驱动的菜单、页面和按钮级访问控制”展开。

项目当前包含：

- `backend`：Spring Boot 3 后端，提供认证、权限、用户、角色、任务、机器人等 API
- `frontend`：Vue 3 + Vite 管理后台
- `e2e`：Playwright 端到端测试
- `start-dev.ps1`：Windows 下的一键开发启动脚本

## 核心特性

- JWT 登录认证
- 基于权限树的菜单、页面、按钮级访问控制
- 用户管理、角色管理、权限管理
- 任务管理、机器人管理、仪表盘概览
- 用户级权限覆盖能力
- Playwright E2E 测试脚手架

当前已经实现的主要页面：

- 登录页
- 仪表盘
- 用户管理
- 角色管理
- 权限管理
- 任务管理
- 机器人管理
- 个人中心

当前以占位页形式保留的模块：

- 流程定义与设计
- 执行监控与日志
- 统计分析
- 系统设置

## 技术栈

### 后端

- Java 17
- Spring Boot 3.2
- Spring Security
- Spring Data JPA
- MySQL 8.0
- H2（测试环境）
- JWT
- SpringDoc OpenAPI

### 前端

- Vue 3
- TypeScript
- Vite 5
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

### 测试

- JUnit 5
- Spring Boot Test
- Playwright

## 目录结构

```text
RPA/
├─ backend/              # Spring Boot 后端
│  ├─ src/main/java/     # 业务代码
│  ├─ src/main/resources/# 配置与初始化 SQL
│  └─ src/test/java/     # 后端测试
├─ frontend/             # Vue 3 前端
│  ├─ src/api/           # API 封装
│  ├─ src/stores/        # Pinia 状态管理
│  ├─ src/router/        # 路由
│  ├─ src/views/         # 页面
│  └─ src/components/    # 通用组件
├─ e2e/                  # Playwright 测试
└─ start-dev.ps1         # 开发环境启动脚本
```

## 运行环境

推荐环境：

- Java 17
- Node.js 18 及以上
- npm 9 及以上
- MySQL 8.0
- Windows PowerShell 5.1+ 或 PowerShell 7+

## 默认配置

### 后端

- 服务地址：`http://localhost:8080/api`
- 默认数据库：
  - URL：`jdbc:mysql://localhost:3306/rpa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai`
  - 用户名：`root`
  - 密码：`root`
- JWT 默认密钥可通过 `JWT_SECRET` 覆盖

### 前端

- 本地开发地址：`http://127.0.0.1:5173`
- 默认 API 地址：`http://localhost:8080/api`
- 可通过环境变量 `VITE_API_BASE_URL` 覆盖后端地址

## 默认账号

应用启动后会自动初始化演示数据，默认可用账号如下：

- 管理员：`admin / admin123`
- 操作员：`user01 / user123`
- 操作员：`user02 / user123`

## 快速开始

第一次上手建议直接看根目录的 [QUICKSTART.md](./QUICKSTART.md)。

如果你已经准备好了 MySQL，可直接在项目根目录执行：

```powershell
.\start-dev.ps1
```

脚本会：

- 为后端注入 MySQL 连接信息
- 启动 Spring Boot 服务
- 检查前端依赖并自动执行 `npm install`
- 启动 Vite 开发服务器

启动完成后访问：

- 前端：`http://127.0.0.1:5173`
- 后端：`http://localhost:8080/api`

## 手动启动

### 1. 启动后端

```powershell
cd D:\aaa\RPA\backend
.\mvnw.cmd spring-boot:run
```

如果需要指定数据库：

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/rpa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "root"
.\mvnw.cmd spring-boot:run
```

### 2. 启动前端

```powershell
cd D:\aaa\RPA\frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

如果后端不是本机默认地址，可以先设置：

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080/api"
npm run dev -- --host 127.0.0.1 --port 5173
```

## 测试与构建

### 后端测试

```powershell
cd D:\aaa\RPA\backend
.\mvnw.cmd test
```

后端测试默认使用 H2 内存数据库，不依赖本地 MySQL。

### 前端类型检查与构建

```powershell
cd D:\aaa\RPA\frontend
npm run typecheck
npm run build
```

### E2E 测试

```powershell
cd D:\aaa\RPA\e2e
npm install
npm run install:browsers
npm test
```

如果要对真实前端地址执行测试：

```powershell
$env:E2E_BASE_URL = "http://127.0.0.1:5173"
npm test
```

## 主要接口

当前主要 REST API 分组如下：

- `/auth`：登录、登出、当前用户
- `/dashboard`：首页概览
- `/users`：用户管理、用户状态、密码、权限覆盖
- `/roles`：角色管理、角色权限分配
- `/permissions`：权限树与权限管理
- `/tasks`：任务管理、启动、停止、状态变更
- `/robots`：机器人管理、启动、停止、状态变更

如果后端已正常启动，可访问 Swagger UI：

- `http://localhost:8080/api/swagger-ui/index.html`

## 数据初始化说明

应用启动时会自动执行初始化 SQL，并通过 `DataInitializer` 补充演示数据，包括：

- 权限树
- 管理员角色与操作员角色
- 默认用户
- 示例机器人
- 示例任务

这让项目在本地开发环境中可以直接登录和演示。

## 开发建议

- 优先使用 `start-dev.ps1` 启动本地联调环境
- 前端联调时通过 `VITE_API_BASE_URL` 切换 API 地址
- 后端接口变更后，可用 Swagger UI 快速验证
- 提交 UI 交互改动时，建议同步补充 `e2e/tests` 中的覆盖

## 已知现状

- 当前部分业务模块仍为占位页，适合继续迭代扩展
- 演示数据适用于本地开发与功能演示，不建议直接用于生产环境
- 开发默认依赖 MySQL，测试环境使用 H2
