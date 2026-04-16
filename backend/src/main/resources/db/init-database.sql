-- ============================================
-- RPA管理系统 完整数据库初始化脚本（DDL + 初始数据）
-- 用途：MySQL 环境一键初始化（建表 + 写入默认用户/角色/权限数据）
-- 数据库：MySQL 8.0+
-- 注意：schema.sql 仅含 DDL，此文件额外包含初始化数据
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 用户表 (sys_user)
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `role` VARCHAR(20) NOT NULL COMMENT '用户角色：ADMIN-管理员，USER-普通用户',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 角色表 (sys_role)
-- ============================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    `description` VARCHAR(200) COMMENT '角色描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_code` (`code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ============================================
-- 3. 权限表 (sys_permission)
-- ============================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '权限名称',
    `code` VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    `type` VARCHAR(20) NOT NULL DEFAULT 'menu' COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父级ID',
    `path` VARCHAR(200) COMMENT '路由路径',
    `icon` VARCHAR(50) COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
    `description` VARCHAR(200) COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_code` (`code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源表';

-- ============================================
-- 4. 角色-权限关联表 (sys_role_permission)
-- ============================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`role_id`, `permission_id`),
    
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================================
-- 5. 机器人表 (sys_robot)
-- ============================================
DROP TABLE IF EXISTS `sys_robot`;
CREATE TABLE `sys_robot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `robot_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '机器人编号',
    `name` VARCHAR(100) NOT NULL COMMENT '机器人名称',
    `type` VARCHAR(50) NOT NULL COMMENT '机器人类型',
    `description` VARCHAR(500) COMMENT '机器人描述',
    `status` VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态：online-在线，offline-离线，running-运行中',
    `total_tasks` BIGINT NOT NULL DEFAULT 0 COMMENT '执行任务总数',
    `success_tasks` BIGINT NOT NULL DEFAULT 0 COMMENT '成功任务数',
    `failed_tasks` BIGINT NOT NULL DEFAULT 0 COMMENT '失败任务数',
    `success_rate` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '成功率',
    `last_execute_time` DATETIME COMMENT '最后执行时间',
    `create_by` BIGINT COMMENT '创建者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_robot_code` (`robot_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人表';

-- ============================================
-- 6. 任务表 (sys_task)
-- ============================================
DROP TABLE IF EXISTS `sys_task`;
CREATE TABLE `sys_task` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_id` VARCHAR(50) UNIQUE COMMENT '任务编号',
    `name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `type` VARCHAR(50) COMMENT '任务类型',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending-等待，running-执行中，completed-完成，failed-失败',
    `progress` INT NOT NULL DEFAULT 0 COMMENT '执行进度（0-100）',
    `robot_id` BIGINT COMMENT '执行机器人ID',
    `robot_name` VARCHAR(50) COMMENT '执行机器人名称',
    `params` LONGTEXT COMMENT '任务参数(JSON)',
    `priority` VARCHAR(20) DEFAULT 'medium' COMMENT '优先级：high-高，medium-中，low-低',
    `execute_type` VARCHAR(20) DEFAULT 'immediate' COMMENT '执行方式：immediate-立即，scheduled-定时',
    `scheduled_time` DATETIME COMMENT '计划执行时间',
    `start_time` DATETIME COMMENT '实际开始时间',
    `end_time` DATETIME COMMENT '实际结束时间',
    `duration` INT DEFAULT 0 COMMENT '执行耗时（秒）',
    `user_id` BIGINT COMMENT '创建用户ID',
    `user_name` VARCHAR(50) COMMENT '创建用户名',
    `description` LONGTEXT COMMENT '任务描述',
    `result` LONGTEXT COMMENT '执行结果',
    `error_message` LONGTEXT COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `tax_id` VARCHAR(50) COMMENT '税号',
    `enterprise_name` VARCHAR(200) COMMENT '企业名称',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_robot_id` (`robot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ============================================
-- 7. 执行日志表 (sys_execution_log)
-- ============================================
DROP TABLE IF EXISTS `sys_crawl_result`;
CREATE TABLE `sys_crawl_result` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_record_id` BIGINT COMMENT '任务主表ID',
    `task_id` VARCHAR(50) NOT NULL UNIQUE COMMENT '任务编号',
    `task_name` VARCHAR(100) COMMENT '任务名称',
    `final_url` VARCHAR(1000) COMMENT '最终URL',
    `title` VARCHAR(500) COMMENT '页面标题',
    `summary_text` LONGTEXT COMMENT '正文摘要',
    `raw_html` LONGTEXT COMMENT '原始HTML',
    `structured_data` LONGTEXT COMMENT '结构化结果',
    `total_count` INT DEFAULT 0 COMMENT '结果条数',
    `crawled_pages` INT DEFAULT 0 COMMENT '抓取页数',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态',
    `error_message` LONGTEXT COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX `idx_crawl_result_task_record_id` (`task_record_id`),
    INDEX `idx_crawl_result_status` (`status`),
    INDEX `idx_crawl_result_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='真实网站抓取结果表';

DROP TABLE IF EXISTS `sys_execution_log`;
CREATE TABLE `sys_execution_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `task_code` VARCHAR(50) COMMENT '任务编号',
    `task_name` VARCHAR(100) COMMENT '任务名称',
    `robot_id` BIGINT COMMENT '机器人ID',
    `robot_name` VARCHAR(50) COMMENT '机器人名称',
    `level` VARCHAR(20) NOT NULL DEFAULT 'INFO' COMMENT '日志级别：INFO, WARN, ERROR',
    `message` TEXT NOT NULL COMMENT '日志内容',
    `stage` VARCHAR(50) COMMENT '执行阶段：start, process, end, error',
    `extra_data` TEXT COMMENT '额外数据（JSON格式）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_task_code` (`task_code`),
    INDEX `idx_robot_id` (`robot_id`),
    INDEX `idx_level` (`level`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行日志表';

-- ============================================
-- 8. 采集配置表 (sys_collect_config)
-- ============================================
DROP TABLE IF EXISTS `sys_collect_config`;
CREATE TABLE `sys_collect_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `task_id` BIGINT COMMENT '关联任务ID',
    `robot_id` BIGINT COMMENT '关联机器人ID',
    `collect_type` VARCHAR(20) NOT NULL DEFAULT 'web' COMMENT '采集类型：web-网页, api-接口, database-数据库',
    `target_url` VARCHAR(500) COMMENT '目标URL',
    `request_method` VARCHAR(10) DEFAULT 'GET' COMMENT '请求方法：GET, POST',
    `request_headers` TEXT COMMENT '请求头（JSON）',
    `request_params` TEXT COMMENT '请求参数（JSON）',
    `request_body` TEXT COMMENT '请求体',
    `collect_rules` TEXT COMMENT '采集规则（JSON）',
    `field_mapping` TEXT COMMENT '字段映射（JSON）',
    `data_clean_rules` TEXT COMMENT '数据清洗规则（JSON）',
    `page_config` TEXT COMMENT '分页配置（JSON）',
    `cron_expression` VARCHAR(100) COMMENT '定时表达式（Cron）',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `timeout` INT DEFAULT 30000 COMMENT '超时时间（毫秒）',
    `retry_count` INT DEFAULT 3 COMMENT '重试次数',
    `proxy_config` TEXT COMMENT '代理配置（JSON）',
    `output_config` TEXT COMMENT '输出配置（JSON）',
    `spider_config` TEXT COMMENT '税务专用 spider 配置',
    `last_execute_time` DATETIME COMMENT '最后执行时间',
    `last_execute_status` VARCHAR(20) COMMENT '最后执行状态',
    `total_count` BIGINT DEFAULT 0 COMMENT '总执行次数',
    `success_count` BIGINT DEFAULT 0 COMMENT '成功次数',
    `fail_count` BIGINT DEFAULT 0 COMMENT '失败次数',
    `create_by` BIGINT COMMENT '创建者ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_robot_id` (`robot_id`),
    INDEX `idx_collect_type` (`collect_type`),
    INDEX `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集配置表';

-- ============================================
-- 9. 采集数据表 (sys_collect_data)
-- ============================================
DROP TABLE IF EXISTS `sys_collect_data`;
CREATE TABLE `sys_collect_data` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `config_id` BIGINT NOT NULL COMMENT '采集配置ID',
    `task_id` BIGINT COMMENT '任务ID',
    `robot_id` BIGINT COMMENT '机器人ID',
    `source_url` VARCHAR(500) COMMENT '数据来源URL',
    `data_hash` VARCHAR(64) COMMENT '数据hash（用于去重）',
    `data_content` TEXT COMMENT '采集的数据内容（JSON）',
    `raw_html` MEDIUMTEXT COMMENT '原始HTML',
    `collect_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
    `status` VARCHAR(20) DEFAULT 'valid' COMMENT '状态：valid-有效, invalid-无效, duplicate-重复',
    `remark` VARCHAR(500) COMMENT '备注',
    
    INDEX `idx_config_id` (`config_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_data_hash` (`data_hash`),
    INDEX `idx_collect_time` (`collect_time`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集数据表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入角色数据
INSERT INTO `sys_role` (`id`, `name`, `code`, `description`, `status`, `sort_order`) VALUES
(1, '管理员', 'ADMIN', '系统管理员，拥有所有权限', 'active', 1),
(2, '普通用户', 'USER', '普通用户，需分配权限', 'active', 2),
(3, '访客', 'GUEST', '访客用户，只有基本查看权限', 'active', 3);

-- 插入用户数据
-- 密码: admin123 (BCrypt加密后的正确值)
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `role`, `status`) VALUES
(1, 'admin', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXvLPbYJlVJNGzMJGZGvUa', '系统管理员', 'admin@example.com', '13800138000', 'ADMIN', 'active'),
(2, 'user01', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXvLPbYJlVJNGzMJGZGvUa', '张三', 'zhangsan@example.com', '13900139001', 'USER', 'active'),
(3, 'user02', '$2a$10$EqKcp1WFKVQISheBxkV3FeYMmM8sOBfKXvLPbYJlVJNGzMJGZGvUa', '李四', 'lisi@example.com', '13900139002', 'USER', 'active');

-- 插入权限数据
INSERT INTO `sys_permission` (`id`, `name`, `code`, `type`, `parent_id`, `path`, `icon`, `sort_order`, `status`, `description`) VALUES
-- 系统管理
(1, '系统管理', 'system:view', 'menu', 0, '/system', 'Setting', 1, 'active', '系统管理模块'),
(2, '用户管理', 'system:user:view', 'menu', 1, '/system/user', 'User', 1, 'active', '用户管理'),
(3, '查看用户', 'system:user:detail', 'button', 2, NULL, NULL, 1, 'active', '查看用户详情'),
(4, '新增用户', 'system:user:add', 'button', 2, NULL, NULL, 2, 'active', '新增用户'),
(5, '编辑用户', 'system:user:edit', 'button', 2, NULL, NULL, 3, 'active', '编辑用户'),
(6, '删除用户', 'system:user:delete', 'button', 2, NULL, NULL, 4, 'active', '删除用户'),
(7, '角色管理', 'system:role:view', 'menu', 1, '/system/role', 'UserFilled', 2, 'active', '角色管理'),
(8, '查看角色', 'system:role:detail', 'button', 7, NULL, NULL, 1, 'active', '查看角色详情'),
(9, '新增角色', 'system:role:add', 'button', 7, NULL, NULL, 2, 'active', '新增角色'),
(10, '编辑角色', 'system:role:edit', 'button', 7, NULL, NULL, 3, 'active', '编辑角色'),
(11, '删除角色', 'system:role:delete', 'button', 7, NULL, NULL, 4, 'active', '删除角色'),
(12, '分配权限', 'system:role:permission', 'button', 7, NULL, NULL, 5, 'active', '分配权限'),
(13, '资源管理', 'system:permission:view', 'menu', 1, '/system/permission', 'Key', 3, 'active', '资源管理'),
-- 任务管理
(14, '任务管理', 'task:view', 'menu', 0, '/task', 'List', 2, 'active', '任务管理模块'),
(15, '任务列表', 'task:list', 'menu', 14, '/task/list', 'Document', 1, 'active', '任务列表'),
(16, '创建任务', 'task:create', 'menu', 14, '/task/create', 'Plus', 2, 'active', '创建任务'),
(17, '任务历史', 'task:history', 'menu', 14, '/task/history', 'Clock', 3, 'active', '任务历史'),
(18, '启动任务', 'task:start', 'button', 15, NULL, NULL, 1, 'active', '启动任务'),
(19, '停止任务', 'task:stop', 'button', 15, NULL, NULL, 2, 'active', '停止任务'),
-- 流程管理
(20, '流程管理', 'workflow:view', 'menu', 0, '/workflow', 'Share', 3, 'active', '流程管理模块'),
(21, '流程列表', 'workflow:list', 'menu', 20, '/workflow/list', 'Document', 1, 'active', '流程列表'),
(22, '流程设计', 'workflow:create', 'menu', 20, '/workflow/design', 'Edit', 2, 'active', '流程设计'),
-- 机器人管理
(23, '机器人管理', 'robot:view', 'menu', 0, '/robot', 'Cpu', 4, 'active', '机器人管理模块'),
(24, '机器人列表', 'robot:list', 'menu', 23, '/robot/list', 'Monitor', 1, 'active', '机器人列表'),
(25, '机器人配置', 'robot:create', 'menu', 23, '/robot/config', 'Setting', 2, 'active', '机器人配置'),
(26, '启动机器人', 'robot:start', 'button', 24, NULL, NULL, 1, 'active', '启动机器人'),
(27, '停止机器人', 'robot:stop', 'button', 24, NULL, NULL, 2, 'active', '停止机器人'),
-- 执行监控
(28, '执行监控', 'monitor:view', 'menu', 0, '/monitor', 'View', 5, 'active', '执行监控模块'),
(29, '实时监控', 'monitor:realtime', 'menu', 28, '/monitor/realtime', 'DataLine', 1, 'active', '实时监控'),
(30, '执行日志', 'monitor:logs', 'menu', 28, '/monitor/logs', 'Tickets', 2, 'active', '执行日志'),
-- 数据统计
(31, '数据统计', 'statistics:view', 'menu', 0, '/statistics', 'DataAnalysis', 6, 'active', '数据统计模块'),
(32, '数据查询', 'statistics:query', 'menu', 31, '/statistics/query', 'Search', 1, 'active', '数据查询'),
(33, '统计报表', 'statistics:report', 'menu', 31, '/statistics/report', 'DataBoard', 2, 'active', '统计报表'),
-- 系统设置
(34, '系统设置', 'settings:view', 'menu', 0, '/settings', 'Tools', 7, 'active', '系统设置模块'),
(35, '基础设置', 'settings:basic:view', 'menu', 34, '/settings/basic', 'Setting', 1, 'active', '基础设置'),
(36, '通知设置', 'settings:notification:view', 'menu', 34, '/settings/notification', 'Bell', 2, 'active', '通知设置');

-- 为管理员角色分配所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`;

-- 插入机器人测试数据
INSERT INTO `sys_robot` (`robot_code`, `name`, `type`, `description`, `status`, `total_tasks`, `success_tasks`, `failed_tasks`, `success_rate`) VALUES
('DC-001', '数据采集机器人-1', 'data_collector', '自动采集网站数据', 'online', 128, 125, 3, 97.66),
('RG-001', '报表生成机器人-1', 'report_generator', '自动生成业务报表', 'online', 85, 85, 0, 100.00),
('TS-001', '任务调度机器人-1', 'task_scheduler', '定时任务调度管理', 'offline', 42, 40, 2, 95.24),
('NT-001', '消息通知机器人-1', 'notification', '发送消息通知', 'offline', 0, 0, 0, 0.00);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 说明：
-- 1. 管理员账号：admin / admin123
-- 2. 测试用户：user01 / user123，user02 / user123
-- 3. 管理员角色已自动分配所有权限
-- 
-- ⚠️ 重要：如果密码无法登录，请删除sys_user表数据，
--    启动应用让系统自动创建正确的用户密码
-- ============================================
