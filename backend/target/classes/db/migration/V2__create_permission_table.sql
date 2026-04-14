-- 创建权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) NOT NULL DEFAULT 'menu' COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    path VARCHAR(200) COMMENT '路由路径',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
    description VARCHAR(200) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源表';

-- 初始化权限数据
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort_order, status, description) VALUES
-- 系统管理 (id=1)
('系统管理', 'system:view', 'menu', 0, '/system', 'Setting', 1, 'active', '系统管理模块'),
-- 用户管理 (id=2-6)
('用户管理', 'system:user:view', 'menu', 1, '/system/user', 'User', 1, 'active', '用户管理'),
('查看用户', 'system:user:detail', 'button', 2, NULL, NULL, 1, 'active', '查看用户详情'),
('新增用户', 'system:user:add', 'button', 2, NULL, NULL, 2, 'active', '新增用户'),
('编辑用户', 'system:user:edit', 'button', 2, NULL, NULL, 3, 'active', '编辑用户'),
('删除用户', 'system:user:delete', 'button', 2, NULL, NULL, 4, 'active', '删除用户'),
-- 角色管理 (id=7-12)
('角色管理', 'system:role:view', 'menu', 1, '/system/role', 'UserFilled', 2, 'active', '角色管理'),
('查看角色', 'system:role:detail', 'button', 7, NULL, NULL, 1, 'active', '查看角色详情'),
('新增角色', 'system:role:add', 'button', 7, NULL, NULL, 2, 'active', '新增角色'),
('编辑角色', 'system:role:edit', 'button', 7, NULL, NULL, 3, 'active', '编辑角色'),
('删除角色', 'system:role:delete', 'button', 7, NULL, NULL, 4, 'active', '删除角色'),
('分配权限', 'system:role:permission', 'button', 7, NULL, NULL, 5, 'active', '分配权限'),
-- 资源管理 (id=13)
('资源管理', 'system:permission:view', 'menu', 1, '/system/permission', 'Key', 3, 'active', '资源管理'),

-- 任务管理 (id=14-19)
('任务管理', 'task:view', 'menu', 0, '/task', 'List', 2, 'active', '任务管理模块'),
('任务列表', 'task:list', 'menu', 14, '/task/list', 'Document', 1, 'active', '任务列表'),
('创建任务', 'task:create', 'menu', 14, '/task/create', 'Plus', 2, 'active', '创建任务'),
('任务历史', 'task:history', 'menu', 14, '/task/history', 'Clock', 3, 'active', '任务历史'),
('启动任务', 'task:start', 'button', 15, NULL, NULL, 1, 'active', '启动任务'),
('停止任务', 'task:stop', 'button', 15, NULL, NULL, 2, 'active', '停止任务'),

-- 流程管理 (id=20-22)
('流程管理', 'workflow:view', 'menu', 0, '/workflow', 'Share', 3, 'active', '流程管理模块'),
('流程列表', 'workflow:list', 'menu', 20, '/workflow/list', 'Document', 1, 'active', '流程列表'),
('流程设计', 'workflow:create', 'menu', 20, '/workflow/design', 'Edit', 2, 'active', '流程设计'),

-- 机器人管理 (id=23-26)
('机器人管理', 'robot:view', 'menu', 0, '/robot', 'Cpu', 4, 'active', '机器人管理模块'),
('机器人列表', 'robot:list', 'menu', 23, '/robot/list', 'Monitor', 1, 'active', '机器人列表'),
('机器人配置', 'robot:create', 'menu', 23, '/robot/config', 'Setting', 2, 'active', '机器人配置'),
('启动机器人', 'robot:start', 'button', 24, NULL, NULL, 1, 'active', '启动机器人'),
('停止机器人', 'robot:stop', 'button', 24, NULL, NULL, 2, 'active', '停止机器人'),

-- 执行监控 (id=27-29)
('执行监控', 'monitor:view', 'menu', 0, '/monitor', 'View', 5, 'active', '执行监控模块'),
('实时监控', 'monitor:realtime', 'menu', 27, '/monitor/realtime', 'DataLine', 1, 'active', '实时监控'),
('执行日志', 'monitor:logs', 'menu', 27, '/monitor/logs', 'Tickets', 2, 'active', '执行日志'),

-- 数据统计 (id=30-32)
('数据统计', 'statistics:view', 'menu', 0, '/statistics', 'DataAnalysis', 6, 'active', '数据统计模块'),
('数据查询', 'statistics:query', 'menu', 30, '/statistics/query', 'Search', 1, 'active', '数据查询'),
('统计报表', 'statistics:report', 'menu', 30, '/statistics/report', 'DataBoard', 2, 'active', '统计报表'),

-- 系统设置 (id=33-35)
('系统设置', 'settings:view', 'menu', 0, '/settings', 'Tools', 7, 'active', '系统设置模块'),
('基础设置', 'settings:basic:view', 'menu', 33, '/settings/basic', 'Setting', 1, 'active', '基础设置'),
('通知设置', 'settings:notification:view', 'menu', 33, '/settings/notification', 'Bell', 2, 'active', '通知设置');

-- 注意：管理员角色的权限需要管理员手动分配，不再预设
-- 普通用户角色的权限也需要管理员手动分配
