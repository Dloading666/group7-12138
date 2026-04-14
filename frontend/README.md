# 管理系统前端

基于 Vue 3 + Element Plus 构建的现代化管理系统前端应用。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Element Plus** - 基于 Vue 3 的组件库
- **Vue Router 4** - Vue.js 官方路由
- **Pinia** - Vue.js 状态管理
- **Vite** - 下一代前端构建工具
- **ECharts** - 数据可视化图表库

## 功能模块

### 1. 系统管理
- 用户管理：用户的增删改查、状态管理
- 角色管理：角色定义、权限分配
- 权限管理：菜单、按钮、接口权限控制

### 2. 任务管理
- 任务列表：查看所有任务、执行控制
- 创建任务：配置任务参数、执行方式
- 任务历史：查看历史执行记录

### 3. 流程定义与设计
- 流程列表：管理所有流程
- 流程设计：可视化流程设计器

### 4. 机器人管理
- 机器人列表：查看机器人状态
- 机器人配置：配置执行参数

### 5. 执行监控与记录
- 实时监控：监控任务执行状态
- 执行日志：查看系统运行日志

### 6. 数据查询与统计
- 数据查询：多条件查询数据
- 统计报表：可视化数据统计

### 7. 系统设置
- 基础设置：系统参数配置
- 通知设置：邮件、短信、Webhook配置

## 快速开始

### 安装依赖

\`\`\`bash
cd frontend
npm install
\`\`\`

### 开发模式

\`\`\`bash
npm run dev
\`\`\`

访问 http://localhost:3000

### 生产构建

\`\`\`bash
npm run build
\`\`\`

### 预览构建

\`\`\`bash
npm run preview
\`\`\`

## 项目结构

\`\`\`
frontend/
├── index.html              # HTML 入口文件
├── package.json            # 项目配置文件
├── vite.config.js          # Vite 配置文件
├── src/
│   ├── main.js            # 应用入口
│   ├── App.vue            # 根组件
│   ├── router/            # 路由配置
│   │   └── index.js
│   ├── views/             # 页面组件
│   │   ├── Login.vue      # 登录页
│   │   ├── Layout.vue     # 布局框架
│   │   ├── Dashboard.vue  # 首页仪表盘
│   │   ├── system/        # 系统管理模块
│   │   ├── task/          # 任务管理模块
│   │   ├── workflow/      # 流程设计模块
│   │   ├── robot/         # 机器人管理模块
│   │   ├── monitor/       # 执行监控模块
│   │   ├── statistics/    # 数据统计模块
│   │   └── settings/      # 系统设置模块
│   └── styles/            # 全局样式
│       └── index.scss
└── public/                # 静态资源
\`\`\`

## 默认登录账号

- 用户名：admin
- 密码：admin123

（注：当前为演示版本，使用模拟数据，无需真实登录验证）

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88

## 开发建议

1. 使用 VSCode 并安装 Volar 插件获得最佳开发体验
2. 建议使用 Node.js 16+ 版本
3. 代码风格遵循 Vue 3 Composition API 规范

## 许可证

MIT License
