-- =============================================
-- 管理系统初始化数据
-- =============================================

-- =============================================
-- 1. 初始化用户数据
-- 密码说明：
-- admin123 的 BCrypt 加密值：$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH
-- user123 的 BCrypt 加密值：$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- =============================================

-- 清空用户表
DELETE FROM sys_user;

-- 插入管理员账号
-- 用户名: admin  密码: admin123
INSERT INTO sys_user (username, password, real_name, email, phone, role, status, avatar) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@example.com', '13800138000', 'ADMIN', 'active', NULL);

-- 插入普通用户账号
-- 用户名: user01  密码: user123
INSERT INTO sys_user (username, password, real_name, email, phone, role, status, avatar) VALUES
('user01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', 'zhangsan@example.com', '13900139001', 'USER', 'active', NULL),
('user02', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四', 'lisi@example.com', '13900139002', 'USER', 'active', NULL),
('user03', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王五', 'wangwu@example.com', '13900139003', 'USER', 'inactive', NULL);

-- =============================================
-- 2. 初始化角色数据
-- =============================================

DELETE FROM sys_role;

INSERT INTO sys_role (name, code, description, status) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限', 'active'),
('普通用户', 'USER', '普通用户，拥有基本权限', 'active'),
('访客', 'GUEST', '访客用户，只有查看权限', 'active');

-- =============================================
-- 3. 初始化权限数据
-- =============================================

DELETE FROM sys_permission;

-- 系统管理
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('系统管理', 'system', 'menu', 0, '/system', 'Setting', 1, 'active'),
('用户管理', 'system:user', 'menu', 1, '/system/user', 'User', 1, 'active'),
('查看用户', 'system:user:view', 'button', 2, NULL, NULL, 1, 'active'),
('添加用户', 'system:user:add', 'button', 2, NULL, NULL, 2, 'active'),
('编辑用户', 'system:user:edit', 'button', 2, NULL, NULL, 3, 'active'),
('删除用户', 'system:user:delete', 'button', 2, NULL, NULL, 4, 'active'),
('角色管理', 'system:role', 'menu', 1, '/system/role', 'UserFilled', 2, 'active'),
('权限管理', 'system:permission', 'menu', 1, '/system/permission', 'Lock', 3, 'active');

-- 任务管理
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('任务管理', 'task', 'menu', 0, '/task', 'List', 2, 'active'),
('任务列表', 'task:list', 'menu', 9, '/task/list', 'Document', 1, 'active'),
('创建任务', 'task:create', 'menu', 9, '/task/create', 'Plus', 2, 'active'),
('执行任务', 'task:execute', 'button', 10, NULL, NULL, 1, 'active');

-- 流程管理
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('流程管理', 'workflow', 'menu', 0, '/workflow', 'Share', 3, 'active'),
('流程列表', 'workflow:list', 'menu', 17, '/workflow/list', 'Document', 1, 'active'),
('流程设计', 'workflow:design', 'menu', 17, '/workflow/design', 'Edit', 2, 'active');

-- 机器人管理
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('机器人管理', 'robot', 'menu', 0, '/robot', 'Cpu', 4, 'active'),
('机器人列表', 'robot:list', 'menu', 22, '/robot/list', 'List', 1, 'active'),
('机器人配置', 'robot:config', 'menu', 22, '/robot/config', 'Setting', 2, 'active');

-- 监控管理
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('执行监控', 'monitor', 'menu', 0, '/monitor', 'Monitor', 5, 'active'),
('实时监控', 'monitor:realtime', 'menu', 28, '/monitor/realtime', 'View', 1, 'active'),
('执行日志', 'monitor:log', 'menu', 28, '/monitor/logs', 'Document', 2, 'active');

-- 数据统计
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('数据统计', 'statistics', 'menu', 0, '/statistics', 'DataAnalysis', 6, 'active'),
('数据查询', 'statistics:query', 'menu', 32, '/statistics/query', 'Search', 1, 'active'),
('统计报表', 'statistics:report', 'menu', 32, '/statistics/report', 'DataLine', 2, 'active');

-- 系统设置
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('系统设置', 'settings', 'menu', 0, '/settings', 'Tools', 7, 'active'),
('基础设置', 'settings:basic', 'menu', 36, '/settings/basic', 'Setting', 1, 'active'),
('通知设置', 'settings:notification', 'menu', 36, '/settings/notification', 'Bell', 2, 'active');

-- =============================================
-- 4. 初始化角色权限关联
-- =============================================

DELETE FROM sys_role_permission;

-- 管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 普通用户权限（查看、任务管理、机器人查看等）
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
-- 任务管理
(2, 9), (2, 10), (2, 11), (2, 12),
-- 流程查看
(2, 13), (2, 14), (2, 15),
-- 机器人查看
(2, 16), (2, 17), (2, 18),
-- 监控查看
(2, 19), (2, 20), (2, 21),
-- 数据统计查看
(2, 22), (2, 23), (2, 24);

-- =============================================
-- 5. 初始化机器人数据
-- =============================================

DELETE FROM sys_robot;

INSERT INTO sys_robot (name, type, status, ip_address, port, task_count, success_rate) VALUES
('Robot-01', '数据采集机器人', 'online', '192.168.1.101', 8080, 128, 98.00),
('Robot-02', '报表生成机器人', 'online', '192.168.1.102', 8080, 85, 100.00),
('Robot-03', '文件处理机器人', 'offline', '192.168.1.103', 8080, 42, 95.00),
('Robot-04', '数据同步机器人', 'offline', '192.168.1.104', 8080, 0, 0.00);

-- =============================================
-- 6. 初始化示例任务数据
-- =============================================

DELETE FROM sys_task;

INSERT INTO sys_task (task_id, name, type, status, progress, robot_id, priority, execute_type, user_id, duration) VALUES
('T001', '数据采集任务A', '数据采集', 'running', 75, 1, 'high', 'immediate', 1, 200),
('T002', '报表生成任务B', '报表生成', 'completed', 100, 2, 'medium', 'immediate', 1, 135),
('T003', '文件处理任务C', '文件处理', 'pending', 0, NULL, 'low', 'scheduled', 2, 0),
('T004', '数据同步任务D', '数据采集', 'failed', 45, 1, 'high', 'immediate', 2, 80);

-- =============================================
-- 查询验证
-- =============================================

SELECT '========== 用户数据 ==========' AS info;
SELECT id, username, real_name, role, status FROM sys_user;

SELECT '========== 角色数据 ==========' AS info;
SELECT id, name, code, description FROM sys_role;

SELECT '========== 权限数据 ==========' AS info;
SELECT id, name, code, type FROM sys_permission LIMIT 10;

SELECT '========== 机器人数据 ==========' AS info;
SELECT id, name, type, status, task_count FROM sys_robot;

SELECT '========== 任务数据 ==========' AS info;
SELECT id, task_id, name, type, status FROM sys_task;
