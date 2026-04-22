-- =============================================
-- MySQL 全量初始化脚本（含 CREATE DATABASE + 建表 + 初始数据）
-- 用途：全新 MySQL 环境一键安装，会自动创建 management_system 数据库
-- 使用方法：mysql -u root -p < init-mysql.sql
-- 与 init-database.sql 的区别：本文件额外包含 CREATE DATABASE 语句
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS management_system 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE management_system;

-- =============================================
-- 1. 用户表
-- =============================================
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN-管理员，USER-普通用户',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
    avatar VARCHAR(255) COMMENT '头像URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 2. 角色表
-- =============================================
DROP TABLE IF EXISTS sys_role;

CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 3. 权限表
-- =============================================
DROP TABLE IF EXISTS sys_permission;

CREATE TABLE sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) NOT NULL COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    path VARCHAR(255) COMMENT '路径',
    icon VARCHAR(50) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_code (code),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- =============================================
-- 4. 角色权限关联表
-- =============================================
DROP TABLE IF EXISTS sys_role_permission;

CREATE TABLE sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =============================================
-- 5. 任务表
-- =============================================
DROP TABLE IF EXISTS sys_task;

CREATE TABLE sys_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    name VARCHAR(100) NOT NULL COMMENT '任务名称',
    type VARCHAR(50) NOT NULL COMMENT '任务类型',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
    progress INT DEFAULT 0 COMMENT '进度(0-100)',
    robot_id BIGINT COMMENT '执行机器人ID',
    robot_name VARCHAR(50) COMMENT '执行机器人名称',
    params LONGTEXT COMMENT '任务参数(JSON)',
    description LONGTEXT COMMENT '任务描述',
    result LONGTEXT COMMENT '执行结果',
    error_message LONGTEXT COMMENT '错误信息',
    priority VARCHAR(20) DEFAULT 'medium' COMMENT '优先级',
    execute_type VARCHAR(20) DEFAULT 'immediate' COMMENT '执行方式',
    scheduled_time DATETIME COMMENT '定时执行时间',
    user_id BIGINT COMMENT '创建用户ID',
    user_name VARCHAR(50) COMMENT '创建用户名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration INT COMMENT '耗时(秒)',
    tax_id VARCHAR(50) COMMENT '税号',
    enterprise_name VARCHAR(200) COMMENT '企业名称',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- =============================================
-- 6. 机器人表
-- =============================================
DROP TABLE IF EXISTS sys_crawl_result;

CREATE TABLE sys_crawl_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '抓取结果ID',
    task_record_id BIGINT COMMENT '任务主表ID',
    task_id VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    task_name VARCHAR(100) COMMENT '任务名称',
    final_url VARCHAR(1000) COMMENT '最终URL',
    title VARCHAR(500) COMMENT '页面标题',
    summary_text LONGTEXT COMMENT '正文摘要',
    raw_html LONGTEXT COMMENT '原始HTML',
    structured_data LONGTEXT COMMENT '结构化结果',
    total_count INT DEFAULT 0 COMMENT '结果条数',
    crawled_pages INT DEFAULT 0 COMMENT '抓取页数',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    error_message LONGTEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_crawl_result_task_record_id (task_record_id),
    INDEX idx_crawl_result_status (status),
    INDEX idx_crawl_result_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='真实网站抓取结果表';

DROP TABLE IF EXISTS sys_robot;

CREATE TABLE sys_robot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '机器人ID',
    name VARCHAR(50) NOT NULL COMMENT '机器人名称',
    type VARCHAR(50) NOT NULL COMMENT '机器人类型',
    status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    port INT COMMENT '端口',
    config TEXT COMMENT '配置信息(JSON)',
    last_heartbeat DATETIME COMMENT '最后心跳时间',
    task_count INT DEFAULT 0 COMMENT '执行任务数',
    success_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '成功率',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机器人表';

-- =============================================
-- 7. 执行日志表
-- =============================================
DROP TABLE IF EXISTS sys_execution_log;

CREATE TABLE sys_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    task_id BIGINT COMMENT '任务ID',
    level VARCHAR(20) NOT NULL COMMENT '日志级别',
    message TEXT COMMENT '日志内容',
    robot_id BIGINT COMMENT '机器人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_task_id (task_id),
    INDEX idx_level (level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行日志表';

-- =============================================
-- 8. 操作日志表
-- =============================================
DROP TABLE IF EXISTS sys_operation_log;

CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作描述',
    method VARCHAR(255) COMMENT '方法名称',
    params TEXT COMMENT '请求参数',
    ip VARCHAR(50) COMMENT 'IP地址',
    status VARCHAR(20) COMMENT '状态',
    error_msg TEXT COMMENT '错误信息',
    duration BIGINT COMMENT '耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 初始化数据
-- =============================================

-- 用户数据
-- 密码: admin123 和 user123 的 BCrypt 加密值
INSERT INTO sys_user (username, password, real_name, email, phone, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin@example.com', '13800138000', 'ADMIN', 'active'),
('user01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三', 'zhangsan@example.com', '13900139001', 'USER', 'active'),
('user02', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四', 'lisi@example.com', '13900139002', 'USER', 'active'),
('user03', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '王五', 'wangwu@example.com', '13900139003', 'USER', 'inactive');

-- 角色数据
INSERT INTO sys_role (name, code, description, status) VALUES
('管理员', 'ADMIN', '系统管理员，拥有所有权限', 'active'),
('普通用户', 'USER', '普通用户，拥有基本权限', 'active'),
('访客', 'GUEST', '访客用户，只有查看权限', 'active');

-- 权限数据
INSERT INTO sys_permission (name, code, type, parent_id, path, icon, sort, status) VALUES
('系统管理', 'system', 'menu', 0, '/system', 'Setting', 1, 'active'),
('用户管理', 'system:user', 'menu', 1, '/system/user', 'User', 1, 'active'),
('角色管理', 'system:role', 'menu', 1, '/system/role', 'UserFilled', 2, 'active'),
('任务管理', 'task', 'menu', 0, '/task', 'List', 2, 'active'),
('任务列表', 'task:list', 'menu', 4, '/task/list', 'Document', 1, 'active'),
('机器人管理', 'robot', 'menu', 0, '/robot', 'Cpu', 4, 'active'),
('执行监控', 'monitor', 'menu', 0, '/monitor', 'Monitor', 5, 'active'),
('数据统计', 'statistics', 'menu', 0, '/statistics', 'DataAnalysis', 6, 'active'),
('系统设置', 'settings', 'menu', 0, '/settings', 'Tools', 7, 'active');

-- 机器人数据
INSERT INTO sys_robot (name, type, status, ip_address, port, task_count, success_rate) VALUES
('Robot-01', '数据采集机器人', 'online', '192.168.1.101', 8080, 128, 98.00),
('Robot-02', '报表生成机器人', 'online', '192.168.1.102', 8080, 85, 100.00),
('Robot-03', '文件处理机器人', 'offline', '192.168.1.103', 8080, 42, 95.00);

-- 示例任务数据
INSERT INTO sys_task (task_id, name, type, status, progress, robot_id, priority, user_id, duration) VALUES
('T001', '数据采集任务A', '数据采集', 'running', 75, 1, 'high', 1, 200),
('T002', '报表生成任务B', '报表生成', 'completed', 100, 2, 'medium', 1, 135),
('T003', '文件处理任务C', '文件处理', 'pending', 0, NULL, 'low', 2, 0);

-- 完成提示
SELECT '数据库初始化完成！' AS message;
SELECT '默认管理员账号: admin / admin123' AS info;
SELECT '普通用户账号: user01 / user123' AS info;
