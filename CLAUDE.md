# CLAUDE.md — RPA 管理系统

## 项目概述

RPA 后台管理系统，前后端分离架构，核心能力为**权限树驱动的菜单/页面/按钮级访问控制**。

- **backend**：Spring Boot 3 + Spring Security + JPA + MySQL
- **frontend**：Vue 3 + TypeScript + Vite + Element Plus + Pinia
- **e2e**：Playwright 端到端测试

## 本地启动

### 一键启动（推荐）

```powershell
.\start-dev.ps1
```

脚本自动注入 MySQL 连接、启动后端、安装前端依赖、启动 Vite。

### 手动启动

```powershell
# 后端
cd backend
.\mvnw.cmd spring-boot:run

# 前端（另开终端）
cd frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

## 默认配置

| 项目 | 值 |
|------|-----|
| 后端地址 | `http://localhost:8080/api` |
| 前端地址 | `http://127.0.0.1:5173` |
| 数据库 URL | `jdbc:mysql://localhost:3306/rpa` |
| 数据库账号 | `root / root` |
| Swagger UI | `http://localhost:8080/api/swagger-ui/index.html` |

默认账号：`admin / admin123`，`user01 / user123`，`user02 / user123`

## 关键目录

```
backend/src/main/java/       # 业务代码（Controller/Service/Repository）
backend/src/main/resources/  # application.yml + 初始化 SQL
frontend/src/api/            # Axios 封装的接口层
frontend/src/stores/         # Pinia 状态管理
frontend/src/views/          # 页面组件
frontend/src/router/         # 路由定义（含权限守卫）
e2e/tests/                   # Playwright 测试用例
```

## 主要 API 分组

| 路径 | 功能 |
|------|------|
| `/auth` | 登录、登出、当前用户 |
| `/dashboard` | 首页概览数据 |
| `/users` | 用户 CRUD、状态、密码、权限覆盖 |
| `/roles` | 角色 CRUD、角色权限分配 |
| `/permissions` | 权限树管理 |
| `/tasks` | 任务管理、启动/停止 |
| `/robots` | 机器人管理、启动/停止 |

## 测试

```powershell
# 后端单元测试（H2 内存库，无需 MySQL）
cd backend && .\mvnw.cmd test

# 前端类型检查与构建
cd frontend && npm run typecheck && npm run build

# E2E 测试
cd e2e && npm install && npm run install:browsers && npm test
```

## 开发规范

- 后端接口变更后用 Swagger UI 验证
- 前端改动通过 `VITE_API_BASE_URL` 环境变量切换后端地址
- 提交 UI 交互改动时，同步补充 `e2e/tests/` 覆盖
- 后端测试默认用 H2，不依赖本地 MySQL

## 环境变量

| 变量 | 用途 |
|------|------|
| `SPRING_DATASOURCE_URL` | 覆盖后端数据库连接 |
| `SPRING_DATASOURCE_USERNAME` | 覆盖数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 覆盖数据库密码 |
| `JWT_SECRET` | 覆盖 JWT 签名密钥 |
| `VITE_API_BASE_URL` | 覆盖前端 API 地址 |
| `E2E_BASE_URL` | E2E 测试目标地址 |

## 注意事项

- 演示数据由 `DataInitializer` 自动注入，仅适用于本地开发
- 部分模块（流程设计、执行监控、统计分析、系统设置）当前为占位页
- 生产环境部署前务必更换 JWT 密钥和数据库密码
