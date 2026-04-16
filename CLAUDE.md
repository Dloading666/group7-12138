# RPA 管理平台 — 架构说明

## 系统架构

```
用户浏览器
    │
    ▼
Vue 3 前端 (port 3000)        ← frontend/
    │  Element Plus + Vite
    │
    ▼
Spring Boot 管理后端 (port 8080)  ← backend/
    │  Spring Security + JWT + JPA
    │  负责：用户/权限/任务调度/机器人管理/执行监控
    │
    ├──── 任务类型: crawl ──────────────────▶ Spider Java 执行器 (port 8081)
    │                                              ← spider/spider_exc/
    │     调用: SpiderApiClient.java               爬取网页/税务数据
    │     回调: POST /api/spider/task/callback     执行完成回调给 8080
    │
    └──── 任务类型: ai_workflow ────────────▶ Python Agent (port 5000)
                                                   ← src/
          调用: AgentApiClient.java                LangGraph 工作流引擎
          回调: POST /api/agent/callback           执行完成回调给 8080
```

## 子系统说明

| 目录 | 端口 | 技术栈 | 职责 |
|------|------|--------|------|
| `frontend/` | 3000 | Vue 3 + Element Plus + Vite | 管理 UI |
| `backend/` | 8080 | Spring Boot 3.2 + JPA + JWT | 主控服务：认证、任务调度、状态跟踪 |
| `spider/spider_exc/` | 8081 | Java Spring Boot | 爬虫执行引擎（接受 8080 提交的爬虫任务） |
| `src/` | 5000 | Python + LangGraph + FastAPI | AI 工作流引擎（接受 8080 提交的 AI 任务） |

> `spider/spider_web/` 是 Spider 的独立管理 UI，不属于主系统前端。

## 任务类型路由规则

`backend/.../engine/RobotExecutor.java` 按任务类型（`task.getType()`）分发：

| 任务类型 | 执行引擎 | 客户端类 |
|---------|---------|---------|
| `crawl` / `spider` | Spider Java (8081) | `SpiderApiClient` |
| `ai_workflow` / `workflow` | Python Agent (5000) | `AgentApiClient` |
| `default` / 其他 | 本地模拟执行 | — |

## 数据库

- **DDL**: `backend/src/main/resources/db/schema.sql` — 唯一 DDL 来源（MySQL）
- **初始数据**: `backend/src/main/resources/db/init-database.sql` — 初始用户/角色/权限数据
- **MySQL 专用**: `backend/src/main/resources/db/init-mysql.sql` — MySQL 环境特有配置
- **运行时**: H2 内存数据库（开发），MySQL（生产）

## 快速启动

```bash
# 1. 启动管理后端
cd backend && mvn spring-boot:run

# 2. 启动爬虫执行器（可选）
cd spider/spider_exc && mvn spring-boot:run

# 3. 启动 Python Agent（可选）
cd src && python main.py -m http -p 5000

# 4. 启动前端
cd frontend && npm run dev
```

默认账号：`admin / admin123`

## API 文档

启动后端后访问：http://localhost:8080/api/swagger-ui/index.html

## 编码规范

- DTO 类名使用大写缩写：`TaskDTO`、`RobotDTO`（不用 `TaskDto`）
- 控制器统一挂载在 `/api` 前缀下（由 `WebConfig` 配置）
- 所有接口返回 `ApiResponse<T>` 包装类
- 任务状态流转：`pending` → `running` → `completed` / `failed`
