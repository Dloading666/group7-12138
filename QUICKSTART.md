# Quick Start

这份文档适合第一次接手项目时快速把系统跑起来。

## 5 分钟启动路径

### 1. 准备环境

确保本机已安装：

- Java 17
- Node.js 18+
- npm
- MySQL 8.0
- PowerShell

### 2. 创建数据库

在 MySQL 中创建一个空库：

```sql
CREATE DATABASE rpa CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

默认连接信息如下，如有需要可自行调整：

- Host：`localhost`
- Port：`3306`
- Database：`rpa`
- Username：`root`
- Password：`root`

### 3. 一键启动

在项目根目录执行：

```powershell
cd D:\aaa\RPA
.\start-dev.ps1
```

如果你的数据库账号不是默认值，可以这样传参：

```powershell
.\start-dev.ps1 -DbHost localhost -DbPort 3306 -DbName rpa -DbUser root -DbPassword your_password
```

脚本会自动拉起：

- 后端：`http://localhost:8080/api`
- 前端：`http://127.0.0.1:5173`

## 首次登录

打开前端地址后，使用任一演示账号登录：

- `admin / admin123`
- `user01 / user123`
- `user02 / user123`

建议先用 `admin / admin123`，这样能看到完整菜单和全部权限能力。

## 手动启动方式

如果你想分别启动前后端，可以按下面做。

### 启动后端

```powershell
cd D:\aaa\RPA\backend
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/rpa?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "root"
.\mvnw.cmd spring-boot:run
```

### 启动前端

```powershell
cd D:\aaa\RPA\frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5173
```

如果后端地址不是默认值，先设置：

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080/api"
```

## 常用地址

- 前端首页：`http://127.0.0.1:5173`
- 后端 API 根地址：`http://localhost:8080/api`
- Swagger UI：`http://localhost:8080/api/swagger-ui/index.html`

## 验证是否启动成功

你可以用下面几个信号快速确认系统正常：

1. 打开 `http://127.0.0.1:5173` 能看到登录页。
2. 用 `admin / admin123` 可以成功登录。
3. 登录后能看到仪表盘、用户管理、角色管理、权限管理、任务管理、机器人管理。
4. 打开 `http://localhost:8080/api/swagger-ui/index.html` 能看到接口文档。

## 常用命令

### 后端测试

```powershell
cd D:\aaa\RPA\backend
.\mvnw.cmd test
```

### 前端类型检查

```powershell
cd D:\aaa\RPA\frontend
npm run typecheck
```

### 前端打包

```powershell
cd D:\aaa\RPA\frontend
npm run build
```

### E2E 测试

```powershell
cd D:\aaa\RPA\e2e
npm install
npm run install:browsers
$env:E2E_BASE_URL = "http://127.0.0.1:5173"
npm test
```

## 常见问题

### 前端能打开，但登录失败

优先检查：

- 后端是否真的启动成功
- `VITE_API_BASE_URL` 是否指向了正确的后端地址
- MySQL 是否可连接
- 数据库中是否已成功初始化表和演示数据

### 后端启动时报数据库连接错误

检查：

- MySQL 服务是否已启动
- 数据库 `rpa` 是否存在
- 用户名密码是否正确
- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` 是否传对

### 想先只验证后端，不接 MySQL

当前开发运行默认走 MySQL；但测试命令 `.\mvnw.cmd test` 使用的是 H2 内存数据库，可以先验证后端核心逻辑和认证流程。

## 下一步建议

如果系统已经跑起来，下一步通常是：

1. 先浏览管理员账号下的完整菜单结构。
2. 用 `user01` 再登录一次，对比不同权限下的页面可见性。
3. 查看权限树、角色权限分配、用户权限覆盖这三块能力是否符合你的业务预期。
4. 需要联调或二次开发时，再回到 [README.md](./README.md) 查看完整说明。
